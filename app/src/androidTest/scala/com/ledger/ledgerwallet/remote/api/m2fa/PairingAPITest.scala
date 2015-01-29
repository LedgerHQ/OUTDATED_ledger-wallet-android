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
import com.koushikdutta.async.http.WebSocket
import com.koushikdutta.async.http.WebSocket.StringCallback
import com.koushikdutta.async.http.server.{AsyncHttpServerRequest, AsyncHttpServer}
import com.koushikdutta.async.http.server.AsyncHttpServer.WebSocketRequestCallback
import com.ledger.ledgerwallet.remote.HttpClient
import junit.framework.Assert
import org.json.JSONObject

import scala.util.{Failure, Success}

class PairingAPITest extends InstrumentationTestCase {

  var server: ApiServer = _
  var API: PairingAPI = _

  override def setUp(): Unit = {
    super.setUp()
    server.run()
    API = new PairingAPI(getInstrumentation.getTargetContext, new HttpClient(Uri.parse("http://localhost")))
  }

  override def tearDown(): Unit = {
    server.stop()
    super.tearDown()
  }

  def testShouldPairDevice: Unit = {
    val signal = new CountDownLatch(1)

    API onRequireUserInput {
      case RequirePairingId() => null
    }
    val future = API.startPairingProcess()

    future onComplete {
      case Success(device) => {
        Assert.assertNotNull(device)
        signal.countDown()
      }
      case Failure(ex) => Assert.fail("Failed to pair device " + ex.getMessage)
    }

    signal.await(30, TimeUnit.SECONDS)
  }

}

private sealed class ApiServer {

  val server = new AsyncHttpServer

  def run(): Unit = {

    server.websocket("/pairing", new WebSocketRequestCallback {
      override def onConnected(webSocket: WebSocket, request: AsyncHttpServerRequest): Unit = {
        webSocket.setStringCallback(new StringCallback {
          override def onStringAvailable(s: String): Unit = {
            val r = new JSONObject(s)

            r.getString("type") match {
              case "join" =>
            }

          }
        })
      }
    })

    server.listen(3000)
  }

  def stop(): Unit = server.stop()

}