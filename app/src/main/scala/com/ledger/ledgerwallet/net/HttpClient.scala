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

import java.io.{ByteArrayInputStream, InputStream}

import android.content.Context
import android.net.Uri
import org.json.{JSONArray, JSONObject}
import scala.collection.mutable
import scala.concurrent.{Promise, Future}
import com.ledger.ledgerwallet.net.ResponseHelper._
import HttpRequestExecutor._
import scala.util.{Failure, Success}

class HttpClient(val baseUrl: Uri, val executor: HttpRequestExecutor = HttpRequestExecutor.getDefault()) {

  var defaultReadTimeout = 30 * 1000
  var defaultConnectTimeout = 10 * 1000
  var retryNumber = 3
  var cacheResponses = true
  var followRedirect = true
  var defaultLogger = new BasicHttpRequestLogger()

  private[this] val _defaultHttpHeaders = mutable.Map[String, String]()

  /*
      http post to 'toto' json
   */

  def get(path: String): MutableRequest = execute("GET", path)
  def post(path: String): MutableRequest = execute("POST", path)
  def put(path: String): MutableRequest = execute("PUT", path)
  def delete(path: String): MutableRequest = execute("DELETE", path)

  def execute(method: String,
              path: String)
             (implicit context: Context = null)
  : MutableRequest = {
    new MutableRequest(
      method = method,
      url = baseUrl.buildUpon().appendEncodedPath(path).build()
    )
  }

  def setDefaultHttpHeader(headerField: (String, String)): Unit = {
    synchronized {
      _defaultHttpHeaders += headerField
    }
  }

  private[this] def createResponseBuilder(request: HttpClient#Request): ResponseBuilder = {
    new ResponseBuilder(request)
  }

  class MutableRequest(
    method: String = null,
    url: Uri = null,
    body: InputStream = null,
    headers: Map[String, String] = _defaultHttpHeaders.toMap,
    readTimeout: Int = defaultReadTimeout,
    connectTimeout: Int = defaultConnectTimeout,
    retryNumber: Int = retryNumber,
    cached: Boolean = cacheResponses,
    followRedirect: Boolean = followRedirect,
    successCodes: List[Int] = List.empty,
    failureCodes: List[Int] = List.empty,
    chunkLength: Int = -1,
    requestLogger: HttpRequestLogger = defaultLogger
    ) extends Request(method, url, body, headers, readTimeout, connectTimeout, retryNumber, cached, followRedirect, successCodes, failureCodes, chunkLength, requestLogger) {

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
    def readTimeout(timeout: Int):MutableRequest = copy(readTimeout = timeout)

    def body(inputStream: InputStream): MutableRequest = copy(body = inputStream)
    def body(stringBody: String): MutableRequest = copy(body = new ByteArrayInputStream(stringBody.getBytes))
    def body(jsonBody: JSONObject): MutableRequest = body(jsonBody.toString).header("Content-Type" -> "application/json")
    def body(jsonBody: JSONArray): MutableRequest = body(jsonBody.toString).header("Content-Type" -> "application/json")

    def contentType(contentType: String): MutableRequest = header("Content-Type" -> contentType)

    def streamBody(chunkLength: Int = 0): MutableRequest = copy(chunkLength = chunkLength)

    def success(code: Int): MutableRequest = copy(successCodes = successCodes.::(code))
    def fail(code: Int): MutableRequest = copy(failureCodes = failureCodes.::(code))

    def logger(logger: HttpRequestLogger): MutableRequest = copy(requestLogger = logger)

    def followRedirect(enable: Boolean): MutableRequest = copy(followRedirect = enable)

    private[this] def copy(method: String = this.method,
                           url: Uri = this.url,
                           body: InputStream = this.body,
                           headers: Map[String, String] = this.headers,
                           readTimeout: Int = this.readTimeout,
                           connectTimeout: Int = this.connectTimeout,
                           retryNumber: Int = this.retryNumber,
                           cached: Boolean = this.cached,
                           followRedirect: Boolean = this.followRedirect,
                           successCodes: List[Int] = this.successCodes,
                           failureCodes: List[Int] = this.failureCodes,
                           chunkLength: Int = this.chunkLength,
                           requestLogger: HttpRequestLogger = this.requestLogger): MutableRequest = {
      new MutableRequest(method, url, body, headers, readTimeout, connectTimeout, retryNumber, cached, followRedirect, successCodes, failureCodes, chunkLength, requestLogger)
    }

  }

  class Request(val method: String,
                val url: Uri,
                val body: InputStream,
                val headers: Map[String, String],
                val readTimeout: Int,
                val connectTimeout: Int,
                val retryNumber: Int,
                val cached: Boolean,
                val followRedirect: Boolean,
                val successCodes: List[Int],
                val failureCodes: List[Int],
                val chunkLength: Int,
                val requestLogger: HttpRequestLogger) {

    lazy val response: Future[HttpClient#Response] = {
      val builder = createResponseBuilder(this.toRequest)
      executor.execute(builder)
      requestLogger.onSendRequest(this)
      builder.future.andThen {
        case Success(response) =>
          requestLogger.onRequestSucceed(response)
          requestLogger.onRequestCompleted(response)
          response
        case Failure(cause: HttpException) =>
          requestLogger.onRequestFailed(cause.response, cause)
          requestLogger.onRequestCompleted(cause.response)
          throw cause
      }
    }

    def json: Future[(JSONObject, HttpClient#Response)] = response.json
    def jsonArray: Future[(JSONArray, HttpClient#Response)] = response.jsonArray
    def string: Future[(String, HttpClient#Response)] = response.string
    def bytes: Future[(Array[Byte], HttpClient#Response)] = response.bytes

    def isBodyStreamed = chunkLength > -1
    def toRequest = new Request(method, url, body, headers, readTimeout, connectTimeout, retryNumber, cached, followRedirect, successCodes, failureCodes, chunkLength, requestLogger)
  }

  class Response(
                val statusCode: Int,
                val statusMessage: String,
                val body: InputStream,
                val headers: Map[String, String],
                val bodyEncoding: String,
                val request: HttpClient#Request) {


  }

  class ResponseBuilder(val request: HttpClient#Request) {
    private [this] val buildPromise = Promise[Response]()
    val future = buildPromise.future

    var statusCode: Int = 0
    var statusMessage: String = ""
    var body: InputStream = _
    var headers = Map[String, String]()
    var bodyEncoding = ""

    def failure(cause: Throwable) = {
      val response = toResponse
      buildPromise.failure(new HttpException(request, response, cause))
      response
    }

    def build(): Response = {
      val response = toResponse
      if ((200 <= response.statusCode && response.statusCode < 400) || response.statusCode == 304)
        buildPromise.success(response)
      else
        buildPromise.failure(new HttpException(request, toResponse, new Exception(s"$statusCode $statusMessage")))
      response
    }

    private[this] def toResponse =
      new Response(
        statusCode,
        statusMessage,
        body,
        headers,
        bodyEncoding,
        request
      )

  }

  case class HttpException(request: HttpClient#Request, response:  HttpClient#Response, cause: Throwable) extends Exception
}
