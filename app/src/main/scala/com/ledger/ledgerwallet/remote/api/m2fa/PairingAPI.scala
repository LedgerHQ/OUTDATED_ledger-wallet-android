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
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.{StringData, Close, WebSocket, HttpClient}
import com.ledger.ledgerwallet.utils.logs.Logger
import org.json.{JSONException, JSONObject}
import scala.concurrent.ExecutionContext.Implicits.global
import com.ledger.ledgerwallet.utils.JsonUtils._

import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success}

class PairingAPI(context: Context, httpClient: HttpClient = HttpClient.defaultInstance) {

  private[this] var _promise: Option[Promise[PairedDongle]] = None
  def future = _promise.map(_.future)

  private[this] var _currentState = State.Resting
  private[this] var _onRequireUserInput: (RequiredUserInput) => Future[String] = _
  private[this] var _websocket: Option[WebSocket] = None
  private[this] def websocket = _websocket
  private[this] def websocket_=(websocket: WebSocket): Unit = _websocket = Option(websocket)

  private[this] var _websocketConnectionRetry = 0
  private[this] val NumberOfWebSocketRetry = 3

  private[this] var _pendingPackage: Option[JSONObject] = None

  private[this] var _pairingId: Option[String] = None

  def onRequireUserInput(f: (RequiredUserInput) => Future[String]): Unit =  {

    _onRequireUserInput = f
  }

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
    initiateConnection()
    _promise.get.future
  }

  def abortPairingProcess(): Unit = {
    if (_promise.isDefined) {
      _currentState = State.Resting
      _promise.get.failure(new InterruptedException())
      _promise = None
      websocket foreach { _.close() }
      _websocketConnectionRetry = 0
    }
  }

  def isStarted = _currentState != State.Resting

  private[this] def initiateConnection(): Unit = {
    httpClient.websocket("/2fa/channels") onComplete {
      case Success(websocket) => {
        _websocketConnectionRetry = 0
        if (_pairingId.isDefined) {
           prepareJoinPackage()
        }
        performPairing(websocket)
      }
      case Failure(exception) => {
        _websocketConnectionRetry += 1
        if (_websocketConnectionRetry > NumberOfWebSocketRetry) {
          _promise foreach {
            _.failure(exception)
          }
        }
      }
    }
  }

  private[this] def performPairing(socket: WebSocket): Unit = {
    sendPendingPackage(socket)
    handleCurrentStep(socket)
    socket on {
      case StringData(data) => {
        try {
          val pkg = new JSONObject(data)
          answerToPackage(socket, pkg)
        } catch {
          case jsonException: JSONException => _promise foreach {_.failure(jsonException)}
          case illegalRequestException: IllegalRequestException => _promise foreach {_.failure(illegalRequestException)}
        }
      }
      case Close(ex) => {
        if (_currentState != State.Resting) {
          initiateConnection()
        }
      }
    }
  }

  private[this] def handleCurrentStep(socket: WebSocket): Unit = {
    _currentState match {
      case State.Starting => handleStartingStep(socket)
      case State.Connecting => handleConnectingStep(socket)
    }
  }

  private[this] def handleStartingStep(socket: WebSocket): Unit = {
    _onRequireUserInput(new RequirePairingId) onComplete {
      case Success(pairingId) => {
         if (pairingId == null)
           _promise. foreach {_.failure(new IllegalUserInputException("Pairing id cannot be null"))}
          _pairingId = Some(pairingId)
          _currentState = State.Connecting
        prepareJoinPackage()
        sendPendingPackage(socket)
        handleCurrentStep(socket)
      }
      case Failure(exception) => _promise foreach {_.failure(exception)}
    }
  }

  private[this] def handleConnectingStep(socket: WebSocket): Unit = {
    // Generate KeyPair

    // ECDH Key agreement

    _currentState = State.Challenging
    Logger.d("Sending public key")
    prepareIdentifyPackage(publicKey = "A superb public key")
    sendPendingPackage(socket)
  }

  private[this] def handleChallengingStep(socket: WebSocket, pkg: JSONObject): Unit = {
    // Ask the user to answer to the challenge
    //_onRequireUserInput(new Requi)
    Logger.d("Client received a challenge " + pkg.toString)

    _onRequireUserInput(new RequireChallengeResponse("xpgg")) onComplete {
      case Success(answer) => {
        prepareChallengePackage(answer = answer)
        sendPendingPackage(socket)
      }
      case Failure(ex) => _promise foreach {_ failure ex}
    }
  }

  private[this] def handleNamingStep(socket: WebSocket, pkg: JSONObject): Unit = {
    // Ask the user a name for its dongle
    // Success the promise
    // Close the connection
    Logger.d("Client must give a name " + pkg.toString)
    _onRequireUserInput(new RequireDongleName) onComplete {
      case Success(name) => {
        Logger.d("Got the name, now finalize and success")
        finalizePairing(name)
      }
      case Failure(ex) => _promise foreach {_.failure(ex)}
    }
  }

  private[this] def finalizePairing(dongleName: String): Unit = {
    _promise foreach {_.success(new PairedDongle())}
  }

  private[this] def answerToPackage(socket: WebSocket, pkg: JSONObject): Unit = {
    pkg.getString("type") match {
      case "challenge" => handleChallengingStep(socket, pkg)
      case "pairing" => handleNamingStep(socket, pkg)
      case "repeat" => sendPendingPackage(socket)
    }
  }

  private[this] def sendPendingPackage(socket: WebSocket): Unit = _pendingPackage foreach socket.send

  private[this] def prepareIdentifyPackage(publicKey: String): Unit = preparePackage(Map("type" -> "identify", "public_key" -> publicKey))
  private[this] def prepareJoinPackage(): Unit = preparePackage(Map("type" -> "join", "room" -> _pairingId.get))
  private[this] def prepareChallengePackage(answer: String): Unit = preparePackage(Map("type" -> "challenge", "data" -> answer))

  private[this] def preparePackage(pkg: JSONObject): Unit = {
    _pendingPackage = Option(pkg)
  }

  private object State extends Enumeration {
    type State = Value
    val Resting, Starting, Connecting, Challenging, Naming = Value
  }

}

sealed abstract class RequiredUserInput
sealed case class RequirePairingId() extends RequiredUserInput
sealed case class RequireChallengeResponse(challenge: String) extends RequiredUserInput
sealed case class RequireDongleName() extends RequiredUserInput

sealed class IllegalRequestException(reason: String = null) extends Exception(reason)
sealed class IllegalUserInputException(reason: String = null) extends Exception(reason)