/**
 *
 * WebSocket
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 02/07/15.
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

import android.net.Uri
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.async.http
import com.koushikdutta.async.http.WebSocket.StringCallback
import com.koushikdutta.async.http.{AsyncHttpClient, AsyncHttpRequest}
import co.ledger.wallet.utils.logs.Logger
import org.apache.http.impl.DefaultHttpRequestFactory
import org.json.{JSONArray, JSONObject}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}

class WebSocket(socket: http.WebSocket) {

  socket.setStringCallback(new StringCallback {
    override def onStringAvailable(s: String): Unit = _stringHandler.foreach(_(s))
  })

  socket.setClosedCallback(new CompletedCallback {
    override def onCompleted(ex: Exception): Unit = _closeHandler.foreach(_(ex))
  })

  def send(data: String): Unit = socket.send(data)
  def send(data: Array[Byte]): Unit = socket.send(data)
  def send(json: JSONObject): Unit = send(json.toString)
  def send(json: JSONArray): Unit = send(json.toString)
  def close(): Unit = Future {socket.close()}
  def isOpen = socket.isOpen
  def isClosed = !isOpen
  def onOpen(handler: () => Unit): Unit = _openHandler = Option(handler)
  def onJsonMessage(handler: JSONObject => Unit): Unit = _jsonHandler = Option(handler)
  def onStringMessage(handler: String => Unit): Unit = _stringHandler = Option(handler)
  def onClose(handler: (Throwable) => Unit): Unit = _closeHandler = Option(handler)
  def onError(handler: Throwable => Unit): Unit = _errorHandler = Option(handler)

  private var _openHandler: Option[() => Unit] = None
  private var _jsonHandler: Option[(JSONObject) => Unit] = None
  private var _stringHandler: Option[(String) => Unit] = None
  private var _closeHandler: Option[(Throwable) => Unit] = None
  private var _errorHandler: Option[(Throwable) => Unit] = None

}

object WebSocket {

  implicit val LogTag = "WebSocket"

  private[this] lazy val _client = AsyncHttpClient.getDefaultInstance
  private[this] lazy val _requestFactory = new DefaultHttpRequestFactory

  def connect(uri: Uri): Future[WebSocket] = {
    val promise = Promise[WebSocket]()
    val url = uri.toString.replace("wss://", "https://").replace("ws://", "http://")
    val request = _requestFactory.newHttpRequest("GET", url)
    Logger.d(s"Connecting to $url")
    _client.websocket(AsyncHttpRequest.create(request), null, null).setCallback(new FutureCallback[http.WebSocket] {
      override def onCompleted(ex: Exception, webSocket: http.WebSocket): Unit = {
        if (ex != null || webSocket == null) {
          val cause = Option(ex).getOrElse(new Exception("WebSocket connection failed with no reason (null websocket)"))
          Logger.d(s"Connection to $url failed ${cause.getMessage}")
          promise.failure(cause)
        } else {
          Logger.d(s"Connected to $url")
          promise.success(new WebSocket(webSocket))
        }
      }
    })
    promise.future
  }

}