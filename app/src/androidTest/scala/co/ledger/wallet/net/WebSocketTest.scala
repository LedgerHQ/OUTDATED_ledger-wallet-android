/**
 *
 * WebSocketTest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 03/07/15.
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
package co.ledger.wallet.net

import java.util.concurrent.{TimeUnit, CountDownLatch}

import android.net.Uri
import android.test.InstrumentationTestCase
import co.ledger.wallet.utils.logs.Logger
import junit.framework.Assert
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global
import co.ledger.wallet.common._

class WebSocketTest extends InstrumentationTestCase {

  var signal: CountDownLatch = _
  val uri = Uri.parse("wss://echo.websocket.org")

  override def setUp(): Unit = {
    super.setUp()
    signal = new CountDownLatch(2)
  }

  def testConnectAndEcho(): Unit = {
    val testString = "Ledger Wallet is on Android too!"
    Logger.d(s"Connecting to ${uri.toString}")
    WebSocket.connect(uri).onComplete {
      case Success(ws) =>
        Logger.d(s"Connected to ${uri.toString}")
        ws.onStringMessage((message) => {
          Logger.d(s"Just received a message $message")
          Assert.assertEquals(testString, message)
          signal.countDown()
          ws.onClose((_) => {signal.countDown()})
          ws.close()
        })
        ws.send(testString)
      case Failure(ex) =>
        Logger.d(s"Failed connection to ${uri.toString}")
        ex.printStackTrace()
        throw ex
    }
    signal.await(30, TimeUnit.SECONDS)
    Assert.assertEquals(0, signal.getCount)
  }

  def testConnectAndEchoJson(): Unit = {
    val testJson = json"{foo: ${"bar"}, ledger: ${"wallet"}}"
    Logger.d(s"Connecting to ${uri.toString}")
    WebSocket.connect(uri).onComplete {
      case Success(ws) =>
        Logger.d(s"Connected to ${uri.toString}")
        ws.onJsonMessage((json) => {
          Logger.d(s"Just received a message ${json.toString}")
          Assert.assertEquals(json.get("foo"), testJson.get("foo"))
          Assert.assertEquals(json.get("ledger"), testJson.get("ledger"))
          signal.countDown()
          ws.onClose((_) => {signal.countDown()})
          ws.close()
        })
        ws.send(testJson)
      case Failure(ex) =>
        Logger.d(s"Failed connection to ${uri.toString}")
        ex.printStackTrace()
        throw ex
    }
    signal.await(30, TimeUnit.SECONDS)
    Assert.assertEquals(0, signal.getCount)
  }

  def testShouldFailedConnection(): Unit = {
    WebSocket.connect(Uri.parse("wss://an_uri_that_will_never_handle_websockets.never/ever")).onComplete {
      case Success(ws) => // WTF???
        ws.onClose((error) => Logger.d(s"Received ${error.getMessage}"))
        Assert.fail("It should failed connection")
      case Failure(ex) => {
        Logger.d("Failed to connect")
        ex.printStackTrace()
        signal.countDown()
        signal.countDown()
      }
    }
    signal.await(30, TimeUnit.SECONDS)
    Assert.assertEquals(0, signal.getCount)
  }

}
