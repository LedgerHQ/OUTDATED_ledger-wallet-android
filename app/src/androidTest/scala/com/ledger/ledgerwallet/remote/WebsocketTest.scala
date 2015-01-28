/**
 *
 * WebsocketTest
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
package com.ledger.ledgerwallet.remote

import java.util.concurrent.{TimeUnit, CountDownLatch}

import android.net.Uri
import android.test.InstrumentationTestCase
import junit.framework.Assert
import org.json.JSONObject
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

class WebSocketTest extends InstrumentationTestCase {

  var client: HttpClient = _
  var signal: CountDownLatch = _
  var websocket: WebSocket = _
  override def setUp(): Unit = {
    super.setUp()
    client = new HttpClient(Uri.parse("https://echo.websocket.org"))
    this.signal = new CountDownLatch(2)
    val signal = new CountDownLatch(1)
    client.websocket("") onComplete {
      case Success(websocket) => {
        this.websocket = websocket
        signal.countDown()
      }
      case Failure(ex) => Assert.fail()
    }
    signal.await()
  }

  def testEchoWebsocket: Unit = {
    val testString = "Hello Android"
    websocket on {
      case StringData(s) => {
        Assert.assertEquals(testString, s)
        signal.countDown()
        websocket.close()
      }
      case Close(ex) => signal.countDown()
    }
    websocket.send(testString)
    signal.await(30, TimeUnit.SECONDS)
  }

  def testJsonEchoWebsocket: Unit = {
    val testJson = new JSONObject()
    testJson.put("android", "test")
    testJson.put("value", 16)
    websocket on {
      case StringData(s) => {
        val result = new JSONObject(s)
        Assert.assertEquals(testJson.toString, result.toString)
        websocket.close()
        signal.countDown()
      }
      case Close(ex) => signal.countDown()
    }
    websocket.send(testJson)
    signal.await(30, TimeUnit.SECONDS)
  }

}
