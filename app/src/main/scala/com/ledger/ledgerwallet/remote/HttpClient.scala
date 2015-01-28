/**
 *
 * HttpClient
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

import android.net.Uri
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback
import com.koushikdutta.async.http._
import com.koushikdutta.async.http.body.{JSONObjectBody, AsyncHttpRequestBody}
import org.apache.http.impl.DefaultHttpRequestFactory
import org.apache.http.HttpRequest
import org.apache.http.client.methods.{HttpPost, HttpGet}
import org.json.{JSONArray, JSONObject}

import scala.collection.mutable
import scala.concurrent.{Future, Promise}


class HttpClient(baseUrl: Uri) {

  type ParametersMap = Map[String, String]
  type HeadersMap = Map[String, String]

  private type HttpFuture[T] = com.koushikdutta.async.future.Future[T]

  val headers = new mutable.HashMap[String, String]()
  val _client = AsyncHttpClient.getDefaultInstance

  def getJsonObject(url: String,
                    params: Option[ParametersMap] = None,
                    body: Option[AsyncHttpRequestBody[_]] = None,
                    headers: Option[HeadersMap] = None)
  : Request[JSONObject] = Request.getJsonObject(url, params, body, headers)

  def getJsonArray(url: String,
                   params: Option[ParametersMap] = None,
                   body: Option[AsyncHttpRequestBody[_]] = None,
                   headers: Option[HeadersMap] = None)
  : Request[JSONArray] = Request.getJsonArray(url, params, body, headers)

  def post(url: String,
           params: Option[ParametersMap] = None,
           body: Option[AsyncHttpRequestBody[_]] = None,
           headers: Option[HeadersMap] = None)
  : Request[JSONObject] = Request.post(url, params, body, headers)

  def postJsonObject(url: String,
                     params: Option[ParametersMap] = None,
                     body: Option[JSONObject] = None,
                     headers: Option[HeadersMap] = None)
  : Request[JSONObject] = Request.post(url, params, body.map(new JSONObjectBody(_)), headers)

  def websocket(url: String,
                protocol: Option[String] = None,
                params: Option[ParametersMap] = None,
                headers: Option[HeadersMap] = None)
  : Future[WebSocket] = Request.websocket(url, protocol, params, headers).future

  trait Request[T] {
    def future: Future[T]
    def cancel(): Unit
    def request: AsyncHttpRequest
    def response: Future[AsyncHttpResponse]
  }

  private class RequestImpl[T](_request: AsyncHttpRequest) extends Request[T] {
    var httpFuture: HttpFuture[T] = _
    val resultPromise = Promise[T]()
    val responsePromise = Promise[AsyncHttpResponse]()

    override def future: Future[T] = resultPromise.future

    override def cancel(): Unit = httpFuture.cancel()

    override def request: AsyncHttpRequest = _request

    override def response: Future[AsyncHttpResponse] = responsePromise.future

  }

  private object Request {

    def getJsonObject(url: String, params: Option[ParametersMap], body: Option[AsyncHttpRequestBody[_]], headers: Option[HeadersMap])
    : Request[JSONObject] = executeJsonObject(new HttpGet(url), params, body, headers)

    def getJsonArray(url: String, params: Option[ParametersMap], body: Option[AsyncHttpRequestBody[_]], headers: Option[HeadersMap])
    : Request[JSONArray] = executeJsonArray(new HttpGet(url), params, body, headers)

    def post(url: String, params: Option[ParametersMap], body: Option[AsyncHttpRequestBody[_]], headers: Option[HeadersMap])
    : Request[JSONObject] = executeJsonObject(new HttpPost(url), params, body, headers)

    private[this] def executeJsonObject(httpRequest: HttpRequest,
                                        params: Option[ParametersMap],
                                        body: Option[AsyncHttpRequestBody[_]],
                                        headers: Option[HeadersMap])
    : Request[JSONObject] = {
      val httpAsyncRequest = configureRequest(httpRequest, params, body, headers)
      val request = new RequestImpl[JSONObject](httpAsyncRequest)
      request.httpFuture = _client.executeJSONObject(httpAsyncRequest, new JSONObjectCallback(request))
      request
    }

    private[this] def executeJsonArray(httpRequest: HttpRequest,
                                       params: Option[ParametersMap],
                                       body: Option[AsyncHttpRequestBody[_]],
                                       headers: Option[HeadersMap])
    : Request[JSONArray] = {
      val httpAsyncRequest = configureRequest(httpRequest, params, body, headers)
      val request = new RequestImpl[JSONArray](httpAsyncRequest)
      request.httpFuture = _client.executeJSONArray(httpAsyncRequest, new JSONArrayCallback(request))
      request
    }

    def websocket(url: String,
                  protocol: Option[String],
                  params: Option[ParametersMap],
                  headers: Option[HeadersMap])
    : Request[WebSocket] = {
      val httpAsyncRequest = configureRequest(new HttpGet(url), params, None, headers)

      val request = new RequestImpl[WebSocket](httpAsyncRequest)
      _client.websocket(httpAsyncRequest, protocol.orNull, new WebSocketConnectCallback {
        override def onCompleted(ex: Exception, webSocket: com.koushikdutta.async.http.WebSocket): Unit = {
          if (ex == null) {
            request.responsePromise.success(null)
            request.resultPromise.success(new WebSocket(webSocket))
          } else {
            request.responsePromise.failure(ex)
            request.resultPromise.failure(ex)
          }
        }
      })
      request
    }

    private[this] def configureRequest(request: HttpRequest,
                                       params: Option[ParametersMap],
                                       body: Option[AsyncHttpRequestBody[_]],
                                       headers: Option[HeadersMap]
                                      )
    : AsyncHttpRequest = {
      var requestUri = Uri.parse(request.getRequestLine.getUri)
      if (requestUri.isRelative && !requestUri.toString.equals("/")) {
        requestUri = baseUrl.buildUpon().appendEncodedPath(requestUri.toString).build()
      } else if (requestUri.isRelative) {
        requestUri = baseUrl
      }
      val uriBuilder = requestUri.buildUpon()
      params.foreach { _ foreach {case (key, value) => uriBuilder.appendQueryParameter(key, value.toString)} }
      val configuredRequest = (new DefaultHttpRequestFactory).newHttpRequest(request.getRequestLine.getMethod, uriBuilder.toString)
      HttpClient.this.headers foreach {case (key, value) => configuredRequest.setHeader(key, value) }
      headers foreach {_ foreach {case (key, value) => configuredRequest.setHeader(key, value)}}
      val asyncRequest = AsyncHttpRequest.create(configuredRequest)
      body foreach asyncRequest.setBody
      asyncRequest
    }

    private class JSONObjectCallback(request: RequestImpl[JSONObject]) extends com.koushikdutta.async.http.AsyncHttpClient.JSONObjectCallback {
      override def onCompleted(e: Exception, source: AsyncHttpResponse, result: JSONObject): Unit = {
        if (source == null) {
          request.responsePromise.failure(e)
          request.resultPromise.failure(e)
        } else if (result == null && e != null) {
          request.responsePromise.success(source)
          request.resultPromise.failure(e)
        } else {
          request.responsePromise.success(source)
          request.resultPromise.success(result)
        }
      }
    }

    private class JSONArrayCallback(request: RequestImpl[JSONArray]) extends com.koushikdutta.async.http.AsyncHttpClient.JSONArrayCallback {
      override def onCompleted(e: Exception, source: AsyncHttpResponse, result: JSONArray): Unit = {
        if (source == null) {
          request.responsePromise.failure(e)
          request.resultPromise.failure(e)
        } else if (result == null && e != null) {
          request.responsePromise.success(source)
          request.resultPromise.failure(e)
        } else {
          request.responsePromise.success(source)
          request.resultPromise.success(result)
        }
      }
    }

  }

}
