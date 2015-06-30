/**
 *
 * HttpClient
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/06/15.
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
package com.ledger.ledgerwallet.net

import java.io.{ByteArrayInputStream, OutputStream, InputStream}

import android.content.Context
import android.net.Uri
import com.netaporter.uri.PathPart
import org.json.{JSONArray, JSONObject}

import scala.concurrent.{Promise, Future}

class HttpClient(val baseUrl: Uri, val executor: HttpRequestExecutor = HttpRequestExecutor.getDefault()) {

  var requestTimeout = 30L * 1000L
  var retryNumber = 3
  var cacheResponses = true

  /*
      http post to 'toto' json
   */

  def get(path: String): MutableRequest = execute("GET", path)

  def execute(method: String,
              path: String)
             (implicit context: Context = null)
  : MutableRequest = {
    new MutableRequest(
      method = method,
      url = baseUrl.buildUpon().appendEncodedPath(path).build(),
      timeout = requestTimeout,
      retryNumber = retryNumber,
      cached = cacheResponses
    )
  }

  private[this] def createResponseBuilder(request: HttpClient#Request): ResponseBuilder = {
    new ResponseBuilder(request)
  }


  class MutableRequest(
    method: String = null,
    url: Uri = null,
    body: InputStream = null,
    headers: Map[String, String] = Map.empty,
    timeout: Long = requestTimeout,
    retryNumber: Int = retryNumber,
    cached: Boolean = cacheResponses,
    chunkLength: Int = -1
    ) extends Request(method, url, body, headers, timeout, retryNumber, cached, chunkLength) {

    def path(pathPart: String): MutableRequest = {
      copy(url = url.buildUpon().appendPath(pathPart).build())
    }

    def param(param: (String, Any)): MutableRequest = {
      copy(url = url.buildUpon().appendQueryParameter(param._1, param._2.toString).build())
    }

    def header(header: (String, String)): MutableRequest = {
      copy(headers = headers + header)
    }

    def retry(retryNumber: Int): MutableRequest = copy(retryNumber = retryNumber)
    def cached(enableCache: Boolean): MutableRequest = copy(cached = enableCache)
    def timeout(requestTimeout: Long):MutableRequest = copy(timeout = requestTimeout)

    def body(inputStream: InputStream): MutableRequest = copy(body = inputStream)
    def body(stringBody: String): MutableRequest = copy(body = new ByteArrayInputStream(stringBody.getBytes))
    def body(jsonBody: JSONObject): MutableRequest = body(jsonBody.toString)
    def body(jsonBody: JSONArray): MutableRequest = body(jsonBody.toString)

    def streamBody(chunkLength: Int = 0): MutableRequest = copy(chunkLength = chunkLength)

    def isBodyStreamed = chunkLength > -1

    private[this] def copy(method: String = this.method,
                           url: Uri = this.url,
                           body: InputStream = this.body,
                           headers: Map[String, String] = this.headers,
                           timeout: Long = this.timeout,
                           retryNumber: Int = this.retryNumber,
                           cached: Boolean = this.cached,
                           chunkLength: Int = this.chunkLength): MutableRequest = {
      new MutableRequest(method, url, body, headers, timeout, retryNumber, cached)
    }

  }

  class Request(val method: String,
                val url: Uri,
                val body: InputStream,
                val headers: Map[String, String],
                val timeout: Long,
                val retryNumber: Int,
                val cached: Boolean,
                val chunkLength: Int) {

    lazy val response: Future[HttpClient#Response] = {
      val builder = createResponseBuilder(this)
      executor.execute(builder)
      builder.future
    }

  }

  class Response(
                val statusCode: Int,
                val statusMessage: String,
                val body: InputStream,
                val headers: Map[String, String],
                val request: HttpClient#Request) {

  }

  class ResponseBuilder(val request: HttpClient#Request) {
    private [this] val buildPromise = Promise[Response]()
    val future = buildPromise.future

    var statusCode: Int = 0
    var statusMessage: String = ""
    var body: InputStream = _
    var headers = Map[String, String]()

    def failure(cause: Throwable) = buildPromise.failure(cause)

    def build(): Response = {
      val response = new Response(
        statusCode,
        statusMessage,
        body,
        headers,
        request
      )
      // TODO: Handle failure
      buildPromise.success(response)
      response
    }

  }

}
