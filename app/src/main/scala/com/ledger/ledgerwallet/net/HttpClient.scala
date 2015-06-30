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

import java.io.{OutputStream, InputStream}

import android.content.Context
import android.net.Uri

import scala.concurrent.{Promise, Future}

class HttpClient(val baseUrl: Uri, val executor: HttpRequestExecutor) {

  var requestTimeout = 30L * 1000L
  var retryNumber = 3
  var cacheResponses = true

  /*
      http post to 'toto' json
   */

  def execute(method: String,
              url: String,
              body: InputStream,
              headers: Map[String, String],
              cached: Boolean = cacheResponses
               )
             (implicit context: Context)
  : Request = {
    null
  }

  private[this] def createResponseBuilder(request: HttpClient#Request): ResponseBuilder = {
    new ResponseBuilder(request)
  }

  class Request(val method: String,
                val url: Uri,
                val body: InputStream,
                val headers: Map[String, String],
                val timeout: Long,
                val retryNumber: Int,
                val cached: Boolean,
                val context: Context) {

    private[this] var _chunkLength = -1

    lazy val response: Future[HttpClient#Response] = {
      val builder = createResponseBuilder(this)
      executor.execute(builder)
      builder.future
    }

    def streamBody(chunkLength: Int = 0): HttpClient#Request = {
      _chunkLength = chunkLength
      this
    }

    def isBodyStreamed = _chunkLength > -1
    def chunkLength = _chunkLength
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
      buildPromise.success(response)
      response
    }

  }

}
