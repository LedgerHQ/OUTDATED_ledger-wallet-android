/**
 *
 * HttpClientTest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 30/06/15.
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

import java.util.concurrent.CountDownLatch

import android.net.Uri
import android.test.InstrumentationTestCase
import co.ledger.wallet.utils.logs.Logger
import junit.framework.Assert
import org.json.JSONObject
import scala.collection.JavaConversions._
import scala.util.{Failure, Success}
import scala.concurrent.ExecutionContext.Implicits.global

class HttpClientTest extends InstrumentationTestCase {

  var client: HttpClient = _
  var signal: CountDownLatch = _

  override def setUp(): Unit = {
    super.setUp()
    client = new HttpClient(Uri.parse("http://httpbin.org"))
    signal = new CountDownLatch(1)
  }

  def testGet(): Unit = {
    client
      .get("get")
      .param("Toto" -> 12)
      .json.onComplete {
        case Success((json, response)) =>
          assert("http://httpbin.org/get?Toto=12" == json.get("url"))
          assert(12 == json.getJSONObject("args").getInt("Toto"))
          signal.countDown()
        case Failure(ex) =>
          ex.printStackTrace()
    }
    signal.await()
  }

  def testPost(): Unit = {
    val json = new JSONObject()
    json.put("a_param", "a_value")
    json.put("another_param", 42)
    client
      .post("post")
      .body(json)
      .json.onComplete {
        case Success((result, response)) =>
          val data = new JSONObject(result.getString("data"))
          for (key <- json.keys()) {
            assert(json.get(key) == data.get(key))
          }
          signal.countDown()
        case Failure(ex) =>
          ex.printStackTrace()
    }
    signal.await()
  }

}
