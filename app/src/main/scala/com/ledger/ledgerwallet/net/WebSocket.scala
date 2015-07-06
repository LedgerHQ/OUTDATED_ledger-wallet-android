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
package com.ledger.ledgerwallet.net

import java.net.URI
import javax.net.ssl.SSLContext

import android.net.Uri
import com.koushikdutta.async.callback.CompletedCallback
import com.koushikdutta.async.future.FutureCallback
import com.koushikdutta.async.http.WebSocket.StringCallback
import com.koushikdutta.async.{http, AsyncServer}
import com.koushikdutta.async.http.AsyncHttpClient.WebSocketConnectCallback
import com.ledger.ledgerwallet.utils.logs.Logger
import com.koushikdutta.async.http.{AsyncHttpRequest, AsyncHttpClient}
import org.apache.http.impl.DefaultHttpRequestFactory
import org.json.{JSONArray, JSONObject}

import scala.concurrent.{Promise, Future}
import scala.concurrent.ExecutionContext.Implicits.global

class WebSocket(socket: http.WebSocket) {

  Logger.d(s"Begin WS creation")

  socket.setStringCallback(new StringCallback {
    override def onStringAvailable(s: String): Unit = _stringHandler.foreach(_(s))
  })

  Logger.d(s"1")

  socket.setClosedCallback(new CompletedCallback {
    override def onCompleted(ex: Exception): Unit = _closeHandler.foreach(_(ex))
  })

  Logger.d(s"2")

  def send(data: String): Unit = socket.send(data)

  Logger.d(s"3")

  def send(data: Array[Byte]): Unit = socket.send(data)

  Logger.d(s"4")

  def send(json: JSONObject): Unit = send(json.toString)

  Logger.d(s"5")

  def send(json: JSONArray): Unit = send(json.toString)

  Logger.d(s"6")

  def close(): Unit = Future {socket.close()}

  Logger.d(s"7")

  def isOpen = socket.isOpen

  Logger.d(s"8")

  def isClosed = !isOpen

  Logger.d(s"9")

  def onOpen(handler: () => Unit): Unit = _openHandler = Option(handler)

  Logger.d(s"10")

  def onJsonMessage(handler: JSONObject => Unit): Unit = _jsonHandler = Option(handler)

  Logger.d(s"11")

  def onStringMessage(handler: String => Unit): Unit = _stringHandler = Option(handler)

  Logger.d(s"13")

  def onClose(handler: (Throwable) => Unit): Unit = _closeHandler = Option(handler)

  Logger.d(s"14")

  def onError(handler: Throwable => Unit): Unit = _errorHandler = Option(handler)

  Logger.d(s"15")

  private var _openHandler: Option[() => Unit] = None

  Logger.d(s"16")

  private var _jsonHandler: Option[(JSONObject) => Unit] = None

  Logger.d(s"17")

  private var _stringHandler: Option[(String) => Unit] = None

  Logger.d(s"18")

  private var _closeHandler: Option[(Throwable) => Unit] = None

  Logger.d(s"19")

  private var _errorHandler: Option[(Throwable) => Unit] = None

  Logger.d(s"End WS creation")
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
        if (ex != null) {
          Logger.d(s"Connection to $url failed ${ex.getMessage}")
          promise.failure(ex)
        } else {
          Logger.d(s"Connected to $url")
          promise.success(new WebSocket(webSocket))
        }
      }
    })
    promise.future
  }

}