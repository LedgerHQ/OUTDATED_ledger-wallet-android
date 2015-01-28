/**
 *
 * HttpClientTest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 27/01/15.
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

import java.util.concurrent.CountDownLatch

import android.net.Uri
import android.test.InstrumentationTestCase
import junit.framework.Assert
import org.json.JSONObject
import scala.concurrent.ExecutionContext.Implicits.global
import scala.collection.JavaConversions._

import scala.util.{Failure, Success}

class HttpClientTest extends InstrumentationTestCase {

  var client: HttpClient = _
  var signal: CountDownLatch = _

  override def setUp(): Unit = {
    super.setUp()
    client = new HttpClient(Uri.parse("http://httpbin.org"))
    client.headers("Global-Header") = "LWGLOBAL"
    signal = new CountDownLatch(1)
  }

  def testGetJsonObject(): Unit = {
    val request = client.getJsonObject("/get")
    request.future onComplete {
      case Success(json) => {
        Assert.assertNotNull(json)
        Assert.assertEquals("http://httpbin.org/get", json.get("url"))
        signal.countDown()
      }
      case Failure(e) => Assert.fail("HTTP failed " + e.getMessage)
    }
    signal.await()
  }

  def testGetJsonObjectWithUrlParameters(): Unit = {
    val params = Map(
      "HTTP-Test" -> "Ledger",
      "Ledger-Wallet" -> "Android"
    )
    val request = client.getJsonObject(url = "/get", params = Option(params))
    request.future onComplete {
      case Success(json) => {
        val args = json.getJSONObject("args")
        params foreach {case (key, value) =>
          Assert.assertEquals(value, args.getString(key))
        }
        signal.countDown()
      }
      case Failure(e) => Assert.fail("HTTP failed " + e.getMessage)
    }
    signal.await()
  }

  def testPostJsonObject(): Unit = {
    val json = new JSONObject()
    json.put("a_param", "a_value")
    json.put("another_param", 42)
    val request = client.postJsonObject(url = "/post", body = Some(json))
    request.future onComplete {
      case Success(resultJson) => {
        val data = new JSONObject(resultJson.getString("data"))
        for (key <- json.keys()) {
          Assert.assertEquals(json.get(key), data.get(key))
        }
        signal.countDown()
      }
      case Failure(e) => Assert.fail("HTTP failed " + e.getMessage)
    }
    signal.await()
  }

}
