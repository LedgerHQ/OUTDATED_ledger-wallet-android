/**
 *
 * PairingAPITest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 29/01/15.
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
package co.ledger.wallet.app.api.m2fa

import android.content.Context
import co.ledger.wallet.InstrumentationTestCase
import co.ledger.wallet.core.crypto.ECKeyPair
import co.ledger.wallet.core.security.Keystore
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.legacy.PairingAPI
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.http._
import com.koushikdutta.async.http.server.AsyncHttpServer.WebSocketRequestCallback
import com.koushikdutta.async.http.server.{AsyncHttpServer, AsyncHttpServerRequest}
import junit.framework.Assert
import org.json.{JSONException, JSONObject}
import org.spongycastle.util.encoders.Hex

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Promise, _}
import scala.concurrent.duration._

class PairingAPITest extends InstrumentationTestCase {

  var server: PairingApiServer = _
  var API: PairingAPI = _

  override def setUp(): Unit = {
    super.setUp()
    server = new PairingApiServer
    server.run()
    API = new MockPairingApi(getInstrumentation.getTargetContext)
  }

  override def tearDown(): Unit = {
    server.stop()
    super.tearDown()
  }

  def testShouldPairDevice(): Unit = {
    val answer = (s: String) => {
      val p = Promise[String]()
      p.success(s)
      p.future
    }

    API onRequireUserInput {
      case RequirePairingId() => answer("1Nro9WkpaKm9axmcfPVp79dAJU1Gx7VmMZ")
      case RequireChallengeResponse(challenge) => {
        eval {
          Assert.assertEquals("FyCD", challenge)
          answer("2C05")
        }
      }
      case RequireDongleName() => answer("Test Dongle")
    }

    val device = await(API.startPairingProcess(), 10.seconds)
    Assert.assertNotNull(device)
  }

}

class PairingApiServer(responseDelay: Long = 0) {

  implicit val logTag = "PairingApiServer"
  val server = new AsyncHttpServer
  var websocket: WebSocket = _
  var send: (String) => Unit = (s: String) => {
    Future {
      blocking(Thread.sleep(responseDelay, 0))
      Logger.d("Server sends " + s)
      if (websocket != null)
        websocket.send(s)
    }
  }

  def run(): Unit = {

    server.setErrorCallback(new CompletedCallback {
      override def onCompleted(ex: Exception): Unit = {
        Assert.failSame(ex.getMessage)
      }
    })

    server.listen(5000)
    server.websocket("/2fa/channels", new WebSocketRequestCallback {
      override def onConnected(webSocket: WebSocket, request: AsyncHttpServerRequest): Unit = {
        Logger.d("Connecting \\o/")
        var room: String = null
        websocket = webSocket
        websocket.setClosedCallback(new CompletedCallback {
          override def onCompleted(ex: Exception): Unit = {
            websocket = null
            onDisconnect()
          }
        })
        webSocket.setStringCallback(new WebSocket.StringCallback {
          override def onStringAvailable(s: String): Unit = {
            Logger.d("Data received \\o/ " + s)
            try {
              val r = new JSONObject(s)
              if (r.getString("type") != "join" && room == null) {
                webSocket.close()
                return
              }
              r.getString("type") match {
                case "join" => room = r.getString("room")
                case "identify" => {
                  val a = new JSONObject()
                  a.put("type", "challenge")
                  a.put("data", "ab5a56a93c1ea8647f8a6982869b2d8a914538525d716b0443248e1cc51c3976")
                  a.put("attestation", "0000000000000001")
                  onSendChallenge(a.toString, send)
                }
                case "challenge" => {
                  val a = new JSONObject()
                  a.put("type", "pairing")
                  a.put("is_successful", r.getString("data").equals("844f0cf804cc7a3b8ac235e0872a2779"))
                  onSendPairing(a.toString, send)
                }
              }
            } catch {
              case ex: JSONException => webSocket.close()
            }
          }
        })
      }
    })

  }

  def disconnectClient(): Unit = {
    if (websocket != null)
      websocket.close()
  }

  def sendDisconnect(): Unit = {
    if (websocket != null) {
      val a = new JSONObject()
      a.put("type", "disconnect")
      onSendDisconnect(a.toString, send)
    }
  }

  def stop(): Unit = {
    Logger.d("Stopping server")
    server.stop()
  }

  def onConnected(): Unit = Logger.d("Server: On connected")
  def onJoinRoom(roomName: String): Unit = {}
  def onSendChallenge(s: String, send: (String) => Unit): Unit = send(s)
  def onSendPairing(s: String, send: (String) => Unit): Unit = send(s)
  def onSendDisconnect(s: String, send: (String) => Unit): Unit = send(s)
  def onDisconnect(): Unit = Logger.d("Server: On disconnected")

}

class MockPairingApi(c: Context) extends PairingAPI(c, Keystore.defaultInstance(c)) {
  override def keypair: ECKeyPair = {
    ECKeyPair.create(Hex.decode("dbd39adafe3a007706e61a17e0c56849146cfe95849afef7ede15a43a1984491"))
  }
}