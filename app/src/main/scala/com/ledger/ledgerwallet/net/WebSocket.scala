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
import org.java_websocket.client.{DefaultSSLWebSocketClientFactory, WebSocketClient}
import org.java_websocket.handshake.ServerHandshake
import org.json.{JSONArray, JSONObject}

import scala.concurrent.Future
import HttpRequestExecutor.defaultExecutionContext

import scala.util.Try

class WebSocket(client: WebSocket.CallbackWebSocketClient) {

  client.delegate.onOpen((ServerHandshake) => {
    _openHandler.foreach(_())
  })

  client.delegate.onClose((code, reason, remote) => {
    _closeHandler.foreach(_(code, reason, remote))
  })

  client.delegate.onMessage((message) => {

    def notifyJsonMessage(): Boolean = {
      Try({
        _jsonHandler.get(new JSONObject(message))
      }).isSuccess
    }

    if (_jsonHandler.isEmpty || !notifyJsonMessage())
      _stringHandler.foreach(_(message))
  })

  client.delegate.onError((ex) => {
    _errorHandler.foreach(_(ex))
  })

  def send(data: String): Unit = client.send(data)
  def send(data: Array[Byte]): Unit = client.send(data)
  def send(json: JSONObject): Unit = send(json.toString)
  def send(json: JSONArray): Unit = send(json.toString)

  def close(): Unit = Future {client.close()}

  def isOpen = Option(client.getConnection).exists(_.isOpen)
  def isConnecting = Option(client.getConnection).exists(_.isConnecting)
  def isClosing = Option(client.getConnection).exists(_.isClosing)
  def isClosed = Option(client.getConnection).exists(_.isClosed)

  def onOpen(handler: () => Unit): Unit = _openHandler = Option(handler)
  def onJsonMessage(handler: JSONObject => Unit): Unit = _jsonHandler = Option(handler)
  def onStringMessage(handler: String => Unit): Unit = _stringHandler = Option(handler)
  def onClose(handler: (Int, String, Boolean) => Unit): Unit = _closeHandler = Option(handler)
  def onError(handler: Throwable => Unit): Unit = _errorHandler = Option(handler)

  private var _openHandler: Option[() => Unit] = None
  private var _jsonHandler: Option[(JSONObject) => Unit] = None
  private var _stringHandler: Option[(String) => Unit] = None
  private var _closeHandler: Option[(Int, String, Boolean) => Unit] = None
  private var _errorHandler: Option[(Throwable) => Unit] = None
}

object WebSocket {

  private[this] lazy val _sslContext = {
    val context = SSLContext.getInstance( "TLS" )
    context.init(null, null, null) // Use system trust manager and key manager
    context
  }

  def connect(uri: Uri): Future[WebSocket] = Future {
    val u = new URI(uri.toString)
    val socket = new CallbackWebSocketClient(u)

    if (uri.getScheme == "https" || uri.getScheme == "wss") {
      socket.setWebSocketFactory(new DefaultSSLWebSocketClientFactory(_sslContext))
    }
    socket.connectBlocking()
    new WebSocket(socket)
  }

  private[WebSocket] class CallbackWebSocketClient(uri: URI) extends WebSocketClient(uri) {

    private var _errorHandler: Option[(Exception) => Unit] = None
    private var _messageHandler: Option[(String) => Unit] = None
    private var _closeHandler: Option[(Int, String, Boolean) => Unit] = None
    private var _openHandler: Option[(ServerHandshake) => Unit] = None

    object delegate {

      def onError(handler: (Exception) => Unit): Unit = _errorHandler = Option(handler)
      def onMessage(handler: (String) => Unit): Unit = _messageHandler = Option(handler)
      def onClose(handler: (Int, String, Boolean) => Unit): Unit = _closeHandler = Option(handler)
      def onOpen(handler: (ServerHandshake) => Unit): Unit = _openHandler = Option(handler)
    }

    override def onError(ex: Exception): Unit = _errorHandler.foreach(_(ex))
    override def onMessage(message: String): Unit = _messageHandler.foreach(_(message))
    override def onClose(code: Int, reason: String, remote: Boolean): Unit = _closeHandler.foreach(_(code, reason, remote))
    override def onOpen(handshakedata: ServerHandshake): Unit = _openHandler.foreach(_(handshakedata))
  }

}

abstract class Event
case class Pong(s: String) extends Event
case class StringData(s: String) extends Event
case class Send() extends Event
case class End(ex: Exception) extends Event
case class Close(ex: Exception) extends Event
case class Json(json: JSONObject) extends Event