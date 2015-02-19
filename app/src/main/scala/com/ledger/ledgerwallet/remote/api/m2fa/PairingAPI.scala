/**
 *
 * PairingAPI
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/15.
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
import com.ledger.ledgerwallet.app.Config
import com.ledger.ledgerwallet.crypto.{Crypto, D3ESCBC, ECKeyPair}
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.{StringData, Close, WebSocket, HttpClient}
import com.ledger.ledgerwallet.utils.logs.Logger
import org.json.{JSONException, JSONObject}
import org.spongycastle.util.encoders.Hex
import scala.concurrent.ExecutionContext.Implicits.global
import com.ledger.ledgerwallet.utils.JsonUtils._

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

class PairingAPI(context: Context, httpClient: HttpClient = HttpClient.websocketInstance) {

  implicit val LogTag = "PairingApi"

  private[this] var _promise: Option[Promise[PairedDongle]] = None
  def future = _promise.map(_.future)

  private[this] var _currentState = State.Resting
  private[this] var _onRequireUserInput: (RequiredUserInput) => Future[String] = _
  private[this] var _websocket: Option[WebSocket] = None
  private[this] def websocket = _websocket
  private[this] def websocket_=(websocket: WebSocket): Unit = _websocket = Option(websocket)

  private[this] var _websocketConnectionRetry = 0
  private[this] val NumberOfWebSocketRetry = 0

  private[this] var _pendingPackage: Option[JSONObject] = None

  private[this] var _pairingId: Option[String] = None

  def onRequireUserInput(f: (RequiredUserInput) => Future[String]): Unit =  {
    _onRequireUserInput = f
  }

  // Protocol management

  def startPairingProcess(): Future[PairedDongle] = {
    if (_onRequireUserInput == null) {
      throw new IllegalStateException("No user input provider are setup. " +
        "Please call onRequireUserInput with a valid provider before starting the pairing process")
    }
    if (_currentState != State.Resting) {
      throw new IllegalStateException("A pairing process is already in progress")
    }
    _promise = Some(Promise())
    _currentState = State.Starting
    _onRequireUserInput(new RequirePairingId) onComplete {
      case Success(pairingId) => {
        if (pairingId == null)
          failure(new IllegalUserInputException("Pairing id cannot be null"))
        _pairingId = Some(pairingId)
        initiateConnection()
      }
      case Failure(exception) => failure(exception)
    }
    _promise.get.future
  }

  def abortPairingProcess(): Unit = {
    if (_promise.isDefined) {
      doAbortPairingProcess()
    }
  }

  private[this] def doAbortPairingProcess(): Unit = {
    _currentState = State.Resting
    _promise.foreach(_.failure(new InterruptedException()))
    _promise = None
    websocket foreach { _.close() }
    _websocketConnectionRetry = 0
  }

  def isStarted = _currentState != State.Resting

  private[this] def initiateConnection(): Unit = {
    httpClient.websocket("/2fa/channels") onComplete {
      case Success(websocket) => {
        _websocketConnectionRetry = 0
        performPairing(websocket)
      }
      case Failure(exception) => {
        _websocketConnectionRetry += 1
        if (_websocketConnectionRetry > NumberOfWebSocketRetry) {
          failure(exception)
        }
      }
    }
  }

  private[this] def performPairing(socket: WebSocket): Unit = {
    handleCurrentStep(socket)
    socket on {
      case StringData(data) => {
        try {
          Logger.d("[WS] <== " + data)
          val pkg = new JSONObject(data)
          answerToPackage(socket, pkg)
        } catch {
          case jsonException: JSONException => failure(jsonException)
          case illegalRequestException: IllegalRequestException => failure(illegalRequestException)
        }
      }
      case Close(ex) => failure(ex)
    }
  }

  private[this] def handleCurrentStep(socket: WebSocket): Unit = {
    _currentState match {
      case State.Starting => handleStartingStep(socket)
      case State.Connecting => handleConnectingStep(socket)
      case state =>
    }
  }

  private[this] def handleStartingStep(socket: WebSocket): Unit = {
    _currentState = State.Connecting
    prepareJoinPackage()
    sendPendingPackage(socket)
    handleCurrentStep(socket)
  }

  private[this] def handleConnectingStep(socket: WebSocket): Unit = {
    // Generate KeyPair

    // ECDH Key agreement

    _currentState = State.Challenging
    Logger.d("Sending public key " + keypair.publicKeyHexString)
    prepareIdentifyPackage(publicKey = keypair.publicKeyHexString)
    sendPendingPackage(socket)
  }

  private[this] def handleChallengingStep(socket: WebSocket, pkg: JSONObject): Unit = {
    // Ask the user to answer to the challenge
    //_onRequireUserInput(new Requi)
    Logger.d("Client received a challenge " + pkg.toString)
    val f = Future {
      val d3es = new D3ESCBC(sessionKey)
      val blob = Hex.decode(pkg.getString("data"))
      PairingAPI.computeChallengePackage(d3es, blob)
    }

    f onComplete {
      case Success(challenge) => {
        _pairingKey = challenge.pairingKey
        _sessionNonce = challenge.sessionNonce
        _onRequireUserInput(new RequireChallengeResponse(challenge.keycardChallege)) onComplete {
          case Success(input) => {
            Logger.d("User input: " + input)
            val answer: Array[Byte] = sessionNonce ++ Hex.decode(input.toCharArray.map("0" + _).mkString("")) ++ Array[Byte](0, 0, 0, 0)
            val cryptedAnswer = new D3ESCBC(sessionKey).encrypt(answer)
            prepareChallengePackage(answer = Hex.toHexString(cryptedAnswer))
            sendPendingPackage(socket)
          }
          case Failure(ex) => failure(ex)
        }
      }
      case Failure(ex) => failure(ex)
    }

  }

  private[this] def handleNamingStep(socket: WebSocket, pkg: JSONObject): Unit = {
    // Ask the user a name for its dongle
    // Success the promise
    // Close the connection
    Logger.d("Client must give a name " + pkg.toString)
    if (!pkg.optBoolean("is_successful", false))
    {
      failure(new PairingAPI.WrongChallengeAnswerException)
      return
    }
    _onRequireUserInput(new RequireDongleName) onComplete {
      case Success(name) => {
        Logger.d("Got the name, now finalize and success")
        finalizePairing(name)
      }
      case Failure(ex) => failure(ex)
    }
  }

  private[this] def finalizePairing(dongleName: String): Unit = {
    implicit val context = this.context
    _promise foreach {_.success(PairedDongle.create(_pairingId.get, dongleName, pairingKey))}
  }

  private[this] def answerToPackage(socket: WebSocket, pkg: JSONObject): Unit = {
    pkg.getString("type") match {
      case "challenge" => handleChallengingStep(socket, pkg)
      case "pairing" => handleNamingStep(socket, pkg)
      case "repeat" => sendPendingPackage(socket)
      case "disconnect" => failure(new PairingAPI.ClientCancelledException)
      case _ => Logger.d("Ignore package " + pkg.toString)
    }
  }

  private[this] def sendPendingPackage(socket: WebSocket): Unit = {
    try {
      for (pkg <- _pendingPackage) {
        Logger.d("[WS] ==> " + pkg.toString)
        socket send pkg
      }
    } catch {
      case e: Throwable => e.printStackTrace()
    }
  }

  private[this] def prepareIdentifyPackage(publicKey: String): Unit = preparePackage(Map("type" -> "identify", "public_key" -> publicKey))
  private[this] def prepareJoinPackage(): Unit = preparePackage(Map("type" -> "join", "room" -> _pairingId.get))
  private[this] def prepareChallengePackage(answer: String): Unit = preparePackage(Map("type" -> "challenge", "data" -> answer))

  private[this] def preparePackage(pkg: JSONObject): Unit = {
    _pendingPackage = Option(pkg)
  }

  // Crypto

  private[this] var _keyPair: ECKeyPair = _
  def keypair_=(kp: ECKeyPair): Unit = _keyPair = kp
  def keypair: ECKeyPair = {
    if (_keyPair == null)
      _keyPair = ECKeyPair.generate()
    _keyPair
  }

  private[this] lazy val _sessionKey = Crypto.splitAndXor(keypair.generateAgreementSecret(Config.LedgerAttestationPublicKey))
  def sessionKey = _sessionKey

  private[this] var _pairingKey: Array[Byte] = _
  def pairingKey = _pairingKey
  private[this] var _sessionNonce: Array[Byte] = _
  def sessionNonce = _sessionNonce

  // Internal Helper

  private[this] def failure(cause: Throwable): Unit = {
    _websocket foreach {_.close()}
    val p = _promise
    _promise = None
    p foreach {_.failure(cause)}
    doAbortPairingProcess()
  }

  private object State extends Enumeration {
    type State = Value
    val Resting, Starting, Connecting, Challenging, Naming = Value
  }

}

object PairingAPI {

  class WrongChallengeAnswerException extends Exception
  class ClientCancelledException extends Exception


  class ChallengePackage(val keycardChallege: String, val pairingKey: Array[Byte], val sessionNonce: Array[Byte])

  def computeChallengePackage(d3es: D3ESCBC, blob: Array[Byte]): ChallengePackage = {
    val sessionNonce = blob.slice(0, 8)
    val data = d3es.decrypt(blob.slice(8, blob.length))
    val keycardChallenge = new String(data.slice(0, 4).map((b) => {
      (b + '0'.toByte).asInstanceOf[Byte]
    }))
    val pairingKey = data.slice(4, 20)
    new ChallengePackage(keycardChallenge, pairingKey, sessionNonce)
  }

}

sealed abstract class RequiredUserInput
sealed case class RequirePairingId() extends RequiredUserInput
sealed case class RequireChallengeResponse(challenge: String) extends RequiredUserInput
sealed case class RequireDongleName() extends RequiredUserInput

sealed class IllegalRequestException(reason: String = null) extends Exception(reason)
sealed class IllegalUserInputException(reason: String = null) extends Exception(reason)