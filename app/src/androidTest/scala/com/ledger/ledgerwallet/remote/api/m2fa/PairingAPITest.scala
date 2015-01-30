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
package com.ledger.ledgerwallet.remote.api.m2fa

import java.util.concurrent.{TimeUnit, CountDownLatch}

import android.net.Uri
import android.test.InstrumentationTestCase
import com.koushikdutta.async.LineEmitter.StringCallback
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.async.http._
import com.koushikdutta.async.http.server.{AsyncHttpServerResponse, HttpServerRequestCallback, AsyncHttpServerRequest, AsyncHttpServer}
import com.koushikdutta.async.http.server.AsyncHttpServer.WebSocketRequestCallback
import com.ledger.ledgerwallet.remote.HttpClient
import com.ledger.ledgerwallet.utils.logs.Logger
import junit.framework.Assert
import org.json.{JSONException, JSONObject}
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Promise

import scala.util.{Failure, Success}

class PairingAPITest extends InstrumentationTestCase {

  var server: ApiServer = _
  var API: PairingAPI = _

  override def setUp(): Unit = {
    super.setUp()
    server = new ApiServer
    server.run()
    API = new PairingAPI(getInstrumentation.getTargetContext, new HttpClient(Uri.parse("http://localhost:5000")))
  }

  override def tearDown(): Unit = {
    //server.stop()
    super.tearDown()
  }

  def testShouldPairDevice: Unit = {
    val signal = new CountDownLatch(1)

    val answer = (s: String) => {
      val p = Promise[String]()
      p.success(s)
      p.future
    }

    API onRequireUserInput {
      case RequirePairingId() => answer("1Nro9WkpaKm9axmcfPVp79dAJU1Gx7VmMZ")
      case RequireChallengeResponse(challenge) => answer("4abf2")
      case RequireDongleName() => answer("Test Dongle")
    }

    val future = API.startPairingProcess()
    future onComplete {
      case Success(device) => {
        Assert.assertNotNull(device)
        signal.countDown()
      }
      case Failure(ex) => Assert.fail("Failed to pair device " + ex.getMessage)
    }

    signal.await(555530, TimeUnit.SECONDS)
  }

}

sealed class ApiServer {

  val server = new AsyncHttpServer
  var websocket: WebSocket = _
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
                  a.put("data", "qdwdqwidwjdioqdjiqwjdqjioq")
                  webSocket.send(a.toString)
                }
                case "challenge" => {
                  val a = new JSONObject()
                  a.put("type", "pairing")
                  a.put("is_succesfull", true)
                  webSocket.send(a.toString)
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

  //def stop(): Unit = server.stop()

}