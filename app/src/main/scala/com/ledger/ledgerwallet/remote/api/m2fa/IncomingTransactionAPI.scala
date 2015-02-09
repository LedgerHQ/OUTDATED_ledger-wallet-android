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
package com.ledger.ledgerwallet.remote.api.m2fa

import android.content.Context
import android.os.{Looper, Handler}
import com.ledger.ledgerwallet.bitcoin.BitcoinUtils
import com.ledger.ledgerwallet.crypto.D3ESCBC
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.{StringData, Close, WebSocket, HttpClient}
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._
import com.ledger.ledgerwallet.utils.JsonUtils._
import org.json.{JSONException, JSONObject}
import org.spongycastle.util.encoders.Hex
import scala.concurrent.ExecutionContext.Implicits.global

import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Try, Failure, Success}

class IncomingTransactionAPI(context: Context, client: HttpClient = HttpClient.websocketInstance) {

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
      PairedDongle.all(context) foreach connect
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
    if (_focusedConnection.isEmpty) {

    }
    false
  }

  private def clearFocus(): Unit ={
    _focusedConnection = None
  }

  class IncomingTransaction(connection: Connection,
                            val dongle: PairedDongle,
                            val pin: String,
                            val amount: String,
                            val fees: String,
                            val change: String,
                            val address: String) {

    def accept(): Unit = connection.sendAcceptPackage()
    def reject(): Unit = connection.sendRejectPackage()
    def cancel(): Unit = mainThreadHandler.post(_onCancelled foreach {_()})

    private[this] var _onCancelled: Option[() => Unit] = None
    def onCancelled(callback: () => Unit): Unit = Option(callback)

  }

  private class Connection(val dongle: PairedDongle) {

    private[this] var _isDisconnected = false
    private[this] var _websocket: Option[WebSocket] = None

    def connect(): Unit = {
      client.websocket("/2fa/channels") onComplete {
        case Success(websocket) => onConnect(websocket)
        case Failure(ex) => {
          _retryNumber += 1
          notifyNetworkDisturbance(_retryNumber)
          scheduleRetryConnect()
        }
      }
    }

    def disconnect(): Unit = {
      _isDisconnected = true
      _websocket foreach {_.close()}
    }

    private[this] def onConnect(websocket: WebSocket): Unit = {
      _websocket = Option(websocket)
      sendConnectionPackage()
      websocket on {
        case StringData(data) => {
          try {
            handlePackage(new JSONObject(data))
          } catch {
            case json: JSONException => json.printStackTrace()
            case others: Throwable => throw others
          }
        }
        case Close(ex) => {
          _websocket = None
          scheduleRetryConnect()
        }
      }
    }

    private[this] def handlePackage(json: JSONObject): Unit = {
      json.getString("type") match {
        case "request" => handleRequestPackage(json)
        case "disconnect" => handleDisconnectPackage(json)
      }
    }

    private[this] def handleRequestPackage(json: JSONObject): Unit = {
      val f = Future {
        val pairingKey = dongle.pairingKey(context)
        val data = json.getString("second_factor_data")
        if (pairingKey.isEmpty)
          throw new Exception("No pairing key")
        val d3es = new D3ESCBC(pairingKey.get.secret)
        val decrypted = d3es.decrypt(Hex.decode(data))

        var offset = 0
        val pin = new String(decrypted.slice(offset, 4))
        offset += 4
        val amount = Hex.toHexString(decrypted.slice(offset, 8))
        offset += 8
        val fees = Hex.toHexString(decrypted.slice(offset, 8))
        offset += 8
        val change = Hex.toHexString(decrypted.slice(offset, 8))
        offset += 8
        val address = new String(decrypted.slice(offset + 1, decrypted.apply(offset).asInstanceOf[Int]))
        if (!BitcoinUtils.isAddressValid(address))
          throw new Exception("Invalid address")
        new IncomingTransaction(this, dongle, pin, amount, fees, change, address)
      }
      f.onComplete {
        case Success(incomingTransaction) => {
          if (requestFocus(this))
            notifyIncomingTransaction(incomingTransaction)
        }
        case Failure(ex) => // Nothing
      }
    }

    private[this] def handleDisconnectPackage(json: JSONObject): Unit = {

    }

    def sendAcceptPackage(): Unit = {

    }

    def sendRejectPackage(): Unit = {

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

  def defaultInstance(context: Context, client: HttpClient = HttpClient.websocketInstance): IncomingTransactionAPI = {
    if (_defaultInstance == null)
      _defaultInstance = new IncomingTransactionAPI(context)
    _defaultInstance
  }

}

/*
offset = 0;
alert("PIN " + authData.bytes(offset, 4).toString(ASCII));
offset += 4;
alert("Output amount " + authData.bytes(offset, 8).toString(HEX));
offset += 8;
alert("Fees " + authData.bytes(offset, 8).toString(HEX));
offset += 8;
alert("Change " + authData.bytes(offset, 8).toString(HEX));
offset += 8;
alert("Destination " + authData.bytes(offset + 1, authData.byteAt(offset)).toString(ASCII));
 */