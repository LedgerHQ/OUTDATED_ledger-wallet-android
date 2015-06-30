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

import com.ledger.ledgerwallet.utils.io.IOUtils

import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.util.{Failure, Success, Try}

class BasicHttpRequestExecutor extends HttpRequestExecutor {

  val TimeoutDuration = 1000L
  val BufferSize = 10 * 1024 // ~= 10KB buffer
  val NumberOfThreads = 10
  val Buffers: mutable.Map[Long, Array[Byte]] = mutable.Map[Long, Array[Byte]]()

  implicit val ec = ExecutionContext.fromExecutor(Executors.newFixedThreadPool(NumberOfThreads))

  override def execute(responseBuilder: HttpClient#ResponseBuilder): Unit = Future {
    for (tryNumber <- 0 until responseBuilder.request.retryNumber)
      attemptPerform(responseBuilder) match {
        case Success(_) =>
          responseBuilder.build()
          return
        case Failure(cause) =>
          if (tryNumber + 1 < responseBuilder.request.retryNumber)
            Thread.sleep(tryNumber * TimeoutDuration)
          else
            responseBuilder.failure(cause)
      }
  }

  private[this] def attemptPerform(responseBuilder: HttpClient#ResponseBuilder): Try[_] = {
    val request = responseBuilder.request
    val url = new URL(request.url.toString)
    val connection = url.openConnection().asInstanceOf[HttpURLConnection]
    implicit val buffer = this.buffer

    val result = Try {
      configureConnection(connection, request)
      writeBody(connection, request)

      responseBuilder.statusCode = connection.getResponseCode
      responseBuilder.statusMessage = connection.getResponseMessage
      responseBuilder.body = new BufferedInputStream(connection.getInputStream)

      val headers = mutable.Map[String, String]()
      for (pos <- 0 until connection.getHeaderFields.size()) {
        headers(connection.getHeaderFieldKey(pos)) = connection.getHeaderField(pos)
      }
      responseBuilder.headers = headers.toMap
    }
    connection.disconnect()
    result
  }

  private[this] def configureConnection(connection: HttpURLConnection,
                                        request: HttpClient#Request): Unit = {

    request.headers.foreach {case (k, v) =>
      connection.setRequestProperty(k, v)
    }

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
        IOUtils.copy(in, out, buffer)
        out.close()
        in.close()
      case nothing => // Nothing to do
    }
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
