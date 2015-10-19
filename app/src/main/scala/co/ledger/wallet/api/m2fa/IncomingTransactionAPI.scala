/**
 *
 * IncomingTransactionAPI
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/02/15.
 *
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Ledger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 *
 */
package co.ledger.wallet.api.m2fa

import java.math.BigInteger
import java.util.Date

import android.content.Context
import android.net.Uri
import android.os.{Handler, Looper}
import co.ledger.wallet.app.Config
import co.ledger.wallet.bitcoin.BitcoinUtils
import co.ledger.wallet.crypto.{D3ESCBC, SecretKey}
import co.ledger.wallet.models.PairedDongle
import co.ledger.wallet.net.WebSocket
import co.ledger.wallet.security.Keystore
import co.ledger.wallet.utils.AndroidImplicitConversions._
import co.ledger.wallet.utils.BytesReader
import co.ledger.wallet.utils.JsonUtils._
import co.ledger.wallet.utils.logs.Logger
import org.bitcoinj.core.Address
import org.bitcoinj.params.MainNetParams
import org.json.{JSONException, JSONObject}
import org.spongycastle.util.encoders.Hex

import scala.collection.mutable
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}
import scala.util.{Failure, Success, Try}

class IncomingTransactionAPI(context: Context, keystore: Keystore, webSocketUri: Uri = Config.WebSocketChannelsUri) {

  implicit val DisableLogging = Config.DisableLogging

  private[this] val handler = new Handler()
  private[this] val mainThreadHandler = new Handler(Looper.getMainLooper)
  private[this] val connections = mutable.Map[String, Connection]()
  private[this] var _isRunning = false
  private[this] var _retryNumber = 0
  private[this] var _focusedConnection: Option[Connection] = None

  // Callback Management

  private[this] var _onIncomingTransaction: Option[(IncomingTransactionAPI#IncomingTransaction) => Unit] = None
  def onIncomingTransaction(callback: (IncomingTransactionAPI#IncomingTransaction) => Unit): Unit = {
    _onIncomingTransaction = Option(callback)
  }
  private[this] var _onNetworkDisturbance: Option[(Int) => Unit] = None
  def onNetworkDisturbance(callback: (Int) => Unit) = _onNetworkDisturbance = Option(callback)

  private[this] def notifyIncomingTransaction(tr: IncomingTransaction)
  : Unit = mainThreadHandler.post(_onIncomingTransaction foreach {_(tr)})
  private[this] def notifyNetworkDisturbance(retry: Int): Unit = mainThreadHandler.post(_onNetworkDisturbance foreach {_(retry)})

  def clearCallback(): Unit = {
    _onIncomingTransaction = None
    _onNetworkDisturbance = None
  }

  // Lifecycle

  def listen(): Unit = {
    if (!_isRunning) {
      _isRunning = true
      Future {
        PairedDongle.all(context) foreach connect
        Logger.d("Waiting for transaction [Listen]")
      }
    }
  }

  def stop(): Unit = {
    if (_isRunning) {
      _isRunning = false
      connections foreach {case (id, connection) => connection.disconnect()}
      connections.clear()
    }
  }

  private[this] def connect(pairedDongle: PairedDongle): Unit = {
    val connection = new Connection(pairedDongle)
    connection.connect()
    connections(pairedDongle.id.get) = connection
  }

  private def requestFocus(connection: Connection): Boolean = {
    if (_focusedConnection.isEmpty && _isRunning) {
      _focusedConnection = Option(connection)
      connections.filterNot(_._1 == connection.dongle.id.get) foreach {_._2.disconnect()}
      connections.clear()
      connections(connection.dongle.id.get) = connection
      Logger.d("Locked in transaction")
      return true
    }
    false
  }

  private def clearFocus(): Unit = {
    if (_focusedConnection.isDefined) {
      val focusedConnection = _focusedConnection.get
      _focusedConnection = None
      PairedDongle.all(context).filterNot(_.id.get == focusedConnection.dongle.id.get) foreach connect
      Logger.d("Waiting for transaction")
    }
  }

  abstract class IncomingTransaction(connection: Connection) {
    def dongle: PairedDongle
    def pin: String
    def amount: BigInteger
    def fees: BigInteger
    def change: BigInteger
    def address: String

    val api = IncomingTransactionAPI.this
    val date = new Date()
    private[this] var _done = false

    def accept(): Unit = {
      _done = true
      connection.sendAcceptPackage()
    }

    def reject(): Unit = {
      _done = true
      connection.sendRejectPackage()
    }

    def cancel(): Unit = {
      _done = true
      connection.endFocus()
      mainThreadHandler.post(_onCancelled foreach {_()})
    }

    def isDone = _done

    private[this] var _onCancelled: Option[() => Unit] = None
    def onCancelled(callback: () => Unit): Unit = _onCancelled = Option(callback)
  }

  private[this] class IncomingTransactionImplV1(connection: Connection,
                            val dongle: PairedDongle,
                            val pin: String,
                            val amount: BigInteger,
                            val fees: BigInteger,
                            val change: BigInteger,
                            val address: String) extends IncomingTransaction(connection) {
    Logger.d("Incoming Transaction")
    Logger.d("Address " + address)
    Logger.d("Amount " + amount.toString)
    Logger.d("Change " + change.toString)
    Logger.d("Fees " + fees.toString)

  }

  private[this] class IncomingTransactionImplV2(connection: Connection,
                                                val dongle: PairedDongle,
                                                val pin: String,
                                                val regularCoinVersion: Byte,
                                                val p2shCoinVersion: Byte,
                                                val dongleOpMode: Byte,
                                                val apiVersion: String,
                                                val flags: Byte,
                                                val outputScript: Array[Byte])
    extends IncomingTransaction(connection) {

    class Output {
      var amount: BigInteger = null
      var hash160: Array[Byte] = null
    }

    val reader = new BytesReader(outputScript)
    val outputsCount = reader.readNextVarInt().value.toInt
    val outputs = new Array[Output](outputsCount)

    val networkParam = MainNetParams.get() // Change this to handle other networks

    for (i <- 0 until outputsCount) {
      val output = new Output()
      output.amount = reader.readLeBigInteger(8)
      reader.readNextVarInt() // Script size
      reader.readNextByte() // OP_DUP
      reader.readNextByte() // OP_HASH160
      val hash160Size = reader.readNextByte().toInt
      output.hash160 = reader.read(hash160Size)
      reader.readNextByte() // OP_EQUALVERIFY
      reader.readNextByte() // OP_CHECKSIG

      outputs(i) = output
    }

    override def address: String = new Address(MainNetParams.get(), outputs(0).hash160).toString
    override def amount: BigInteger = outputs(0).amount
    override def fees: BigInteger = BigInteger.ZERO
    override def change: BigInteger = outputs(1).amount
  }

  private class Connection(val dongle: PairedDongle) {
    Logger.d("Connect to room " + dongle.id.get)
    private[this] var _isDisconnected = false
    private[this] var _websocket: Option[WebSocket] = None
    private[this] var _incomingTransaction: Option[IncomingTransaction] = None

    def connect(): Unit = {
      Logger.d("Connecting to " + dongle.id.get)
      WebSocket.connect(webSocketUri) onComplete {
        case Success(websocket) => onConnect(websocket)
        case Failure(ex) => {
          _retryNumber += 1
          notifyNetworkDisturbance(_retryNumber)
          scheduleRetryConnect()
        }
      }
    }

    def disconnect(): Unit = {
      Logger.d("Disconnecting from " + dongle.id.get)
      _isDisconnected = true
      _websocket foreach {_.close()}
    }

    private[this] def onConnect(websocket: WebSocket): Unit = {
      _websocket = Option(websocket)
      sendConnectionPackage()
      websocket onStringMessage((data) => {
        try {
          handlePackage(new JSONObject(data))
        } catch {
          case json: JSONException => json.printStackTrace()
          case others: Throwable => throw others
        }
      })

      websocket onClose((ex) => {
        _websocket = None
        scheduleRetryConnect()
      })

    }

    private[this] def handlePackage(json: JSONObject): Unit = {
      json.getString("type") match {
        case "request" => handleRequestPackage(json)
        case "disconnect" => handleDisconnectPackage(json)
        case _ => Logger.d("Received unknown package " + json.toString)
      }
    }

    private[this] def handleRequestPackage(json: JSONObject): Unit = {
      Logger.d(s"Handle request package ${json.toString}")
      Future {
        val pairingKey = Try(Await.result(dongle.pairingKey(context, keystore), Duration.Inf))
        if (pairingKey.isFailure)
          throw new Exception("No pairing key")
        pairingKey.get
      } flatMap { (pairingKey) =>
        if (json.has("output_data") && !json.getString("output_data").isEmpty) {
          handlePost110RequestPackage(json, pairingKey)
        } else {
          handlePre110RequestPackage(json, pairingKey)
        }
      } onComplete {
        case Success(incomingTransaction) => {
          if (requestFocus(this)) {
            _incomingTransaction = Some(incomingTransaction)
            send(Map("type" -> "accept"))
            notifyIncomingTransaction(incomingTransaction)
          }
        }
        case Failure(ex) => ex.printStackTrace()// The transaction cannot be handled
      }
    }

    private[this] def handlePost110RequestPackage(json: JSONObject, pairingKey: SecretKey): Future[IncomingTransaction] = {
      Future {
        val data = json.getString("second_factor_data")
        val dataD3es = new D3ESCBC(pairingKey.secret)
        val decryptedData = new BytesReader(dataD3es.decrypt(Hex.decode(data)))
        if (decryptedData.length < 30) {
          throw new Exception("Invalid request")
        }
        val version = decryptedData.readString(4)
        if (version != "2FA1")
          throw new Exception("Invalid request")
        val decryptionKey = decryptedData.read(16)
        val CRC16 = decryptedData.read(2)
        val flags = decryptedData.readNextByte()
        val currentDongleOpMode = decryptedData.readNextByte()
        val regularCoinVersion = decryptedData.readNextByte()
        val p2shCoinVersion = decryptedData.readNextByte()
        val pin = decryptedData.readString(4)
        val outputScriptHex = json.getString("output_data")
        val outputD3es = new D3ESCBC(decryptionKey)
        val outputScript = outputD3es.decrypt(Hex.decode(outputScriptHex))
        new IncomingTransactionImplV2(
          connection = this,
          dongle = dongle,
          pin = pin,
          regularCoinVersion = regularCoinVersion,
          p2shCoinVersion = p2shCoinVersion,
          dongleOpMode = currentDongleOpMode,
          apiVersion = version,
          flags = flags,
          outputScript = outputScript
        )
      }
    }

    private[this] def handlePre110RequestPackage(json: JSONObject, pairingKey: SecretKey): Future[IncomingTransaction] = {
      Future {
        val data = json.getString("second_factor_data")
        val d3es = new D3ESCBC(pairingKey.secret)
        val decrypted = d3es.decrypt(Hex.decode(data))
        if (decrypted.length <= 29)
          throw new Exception("Invalid request")
        Logger.d("Package -> " + json.toString)
        Logger.d("Decrypted -> " + Hex.toHexString(decrypted))
        var offset = 0
        val pin = new String(decrypted.slice(offset, offset + 4))
        offset += 4
        val amount = new BigInteger(1, decrypted.slice(offset, offset + 8))
        offset += 8
        val fees = new BigInteger(1, decrypted.slice(offset, offset + 8))
        offset += 8
        val change = new BigInteger(1, decrypted.slice(offset, offset + 8))
        offset += 8
        if (decrypted.length < offset + 1 + decrypted.apply(offset).toInt)
          throw new Exception("Invalid request")
        val address = new String(decrypted.slice(offset + 1, offset + 1 + decrypted.apply(offset).toInt))
        if (!BitcoinUtils.isAddressValid(address))
          throw new Exception("Invalid address")
        new IncomingTransactionImplV1(this, dongle, pin, amount, fees, change, address)
      }
    }

    private[this] def handleDisconnectPackage(json: JSONObject): Unit = {
      if (_incomingTransaction.isDefined) {
        _incomingTransaction.get.cancel()
        endFocus()
      }
    }

    def endFocus(): Unit = {
      _incomingTransaction = None
      clearFocus()
      _isDisconnected = false
    }

    def sendAcceptPackage(): Unit = {
      send(Map("type" -> "response", "is_accepted" -> true, "pin" -> _incomingTransaction.get.pin))
      endFocus()
    }

    def sendRejectPackage(): Unit = {
      send(Map("type" -> "response", "is_accepted" -> false))
      endFocus()
    }

    private[this] def sendConnectionPackage(): Unit = {
      send(Map("type" -> "join", "room" -> dongle.id.get))
      send(Map("type" -> "repeat"))
    }

    private[this] def send(json: JSONObject): Unit = {
      if (_websocket.isDefined)
        _websocket.get.send(json.toString)
    }

    private[this] def scheduleRetryConnect(): Unit = {
      if (!_isDisconnected)
        handler.postDelayed(connect(), 3000)
    }

  }

}

object IncomingTransactionAPI {

  private[this] var _defaultInstance: IncomingTransactionAPI = null

  def defaultInstance(context: Context): IncomingTransactionAPI = {
    if (_defaultInstance == null)
      _defaultInstance = new IncomingTransactionAPI(context, Keystore.defaultInstance(context))
    _defaultInstance
  }

}