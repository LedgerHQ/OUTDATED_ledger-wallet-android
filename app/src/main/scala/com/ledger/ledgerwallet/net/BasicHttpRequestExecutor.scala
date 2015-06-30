/**
 *
 * BasicHttpRequestExecutor
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 29/06/15.
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

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.concurrent.Executors

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success, Try}

class BasicHttpRequestExecutor extends HttpRequestExecutor {

  val BufferSize = 10 * 1024 // ~= 10KB buffer
  val NumberOfThreads = 10
  val Buffers: mutable.Map[Long, Array[Byte]] = mutable.Map[Long, Array[Byte]]()

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(NumberOfThreads))



  override def execute(responseBuilder: HttpClient#ResponseBuilder): Unit = Future {
    val request = responseBuilder.request
    val url = new URL(request.url.toString)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    implicit val buffer = this.buffer

    Try {
      configureConnection(connection, request)
      writeBody(connection, request)

    } match {
      case Success(_) => responseBuilder.build()
      case Failure(cause) => responseBuilder.failure(cause)
    }
    connection.disconnect()

  }

  private[this] def configureConnection(connection: HttpURLConnection,
                                        request: HttpClient#Request): Unit = {
    request.method match {
      case "POST" | "PUT" =>
        connection.setDoOutput(true)
        connection.setRequestMethod(request.method)
        if (request.isBodyStreamed) {
          connection.setChunkedStreamingMode(request.chunkLength)
        } else {
          connection.setFixedLengthStreamingMode(request.body.available())
        }
      case "GET" | "DELETE" =>
        connection.setRequestMethod(request.method)
      case invalid =>
        throw new Exception(s"No such method ${request.method}")
    }
  }

  private[this] def writeBody(connection: HttpURLConnection,
                              request: HttpClient#Request)
                             (implicit buffer: Array[Byte]): Unit = {
    request.method match {
      case "POST" | "PUT" =>
        val out = new BufferedOutputStream(connection.getOutputStream)
        val in = new BufferedInputStream(request.body)

        out.close()
      case nothing => // Nothing to do
    }
  }

  private[this] def readResponse(connection: HttpURLConnection,
                                 request: HttpClient#Request)
                                (implicit buffer: Array[Byte]): Unit = {

  }

  private[this] def buffer: Array[Byte] = {
    Buffers.synchronized {
      var buffer = Buffers(Thread.currentThread().getId)
      if (buffer == null) {
        buffer = new Array[Byte](BufferSize)
        Buffers(Thread.currentThread().getId) = buffer
      }
      buffer
    }
  }

}
