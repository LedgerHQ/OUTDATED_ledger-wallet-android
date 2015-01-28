/**
 *
 * Websocket
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/15.
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

import com.koushikdutta.async.{ByteBufferList, DataEmitter}
import com.koushikdutta.async.callback.{WritableCallback, CompletedCallback, DataCallback}
import com.koushikdutta.async.http.WebSocket._
import org.json.{JSONArray, JSONObject}

class WebSocket(webSocket: com.koushikdutta.async.http.WebSocket) {

  private var _wsCallback: Option[(Event) => Unit] = None

  def send(string: String): Unit = webSocket.send(string)
  def send(data: Array[Byte]): Unit = webSocket.send(data)
  def send(data: Array[Byte], offset: Int, length: Int): Unit = webSocket.send(data, offset, length)
  def send(json: JSONObject): Unit = webSocket.send(json.toString)
  def send(json: JSONArray): Unit = webSocket.send(json.toString)

  def pause(): Unit = webSocket.pause()
  def isPaused = webSocket.isPaused

  def resume(): Unit = webSocket.resume()

  def isOpen = webSocket.isOpen
  def isChunked = webSocket.isChunked
  def isBuffering = webSocket.isBuffering

  def close(): Unit = webSocket.close()
  def end(): Unit = webSocket.end()

  def on(f: (Event) => Unit): Unit = _wsCallback = Option(f)

  webSocket.setPongCallback(new PongCallback {
    override def onPongReceived(s: String): Unit = _wsCallback foreach {_(new Pong(s))}
  })

  webSocket.setStringCallback(new StringCallback {
    override def onStringAvailable(s: String): Unit = _wsCallback foreach {_(new StringData(s))}
  })

  webSocket.setDataCallback(new DataCallback {
    override def onDataAvailable(emitter: DataEmitter, bb: ByteBufferList): Unit = _wsCallback foreach {_(new Data(bb))}
  })

  webSocket.setClosedCallback(new CompletedCallback {
    override def onCompleted(ex: Exception): Unit = _wsCallback foreach {_(new Close(ex))}
  })

  webSocket.setEndCallback(new CompletedCallback {
    override def onCompleted(ex: Exception): Unit = _wsCallback foreach {_(new End(ex))}
  })

  webSocket.setWriteableCallback(new WritableCallback {
    override def onWriteable(): Unit = _wsCallback foreach {_(new Send())}
  })

}

abstract class Event
case class Pong(s: String) extends Event
case class StringData(s: String) extends Event
case class Data(b: ByteBufferList) extends Event
case class Send() extends Event
case class End(ex: Exception) extends Event
case class Close(ex: Exception) extends Event
case class Json(json: JSONObject) extends Event