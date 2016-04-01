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
package co.ledger.wallet.core.net

import java.io._
import java.net.{HttpURLConnection, URL}

import co.ledger.wallet.core.net.HttpRequestExecutor.defaultExecutionContext
import co.ledger.wallet.core.utils.logs.Loggable
import org.apache.commons.io.IOUtils

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class BasicHttpRequestExecutor extends HttpRequestExecutor with Loggable {

  val TimeoutDuration = 1000L
  val BufferSize = 10 * 1024 // ~= 10KB buffer
  val Buffers: mutable.Map[Long, Array[Char]] = mutable.Map[Long, Array[Char]]()

  override def execute(responseBuilder: HttpClient#ResponseBuilder): Unit = Future {
    @tailrec
    def iterate(tryNumber: Int): Unit = {
      var continue = false
      attemptPerform(responseBuilder) match {
        case Success(_) =>
          Try(responseBuilder.request.body.close())
          responseBuilder.build()
        case Failure(cause) =>
          cause.printStackTrace()
          if (tryNumber + 1 < responseBuilder.request.retryNumber && responseBuilder.request.body.markSupported()) {
            Thread.sleep(tryNumber * TimeoutDuration)
            continue = true
          } else
            Try(responseBuilder.request.body.close())
            responseBuilder.failure(cause)
      }
      if (continue && tryNumber + 1 < responseBuilder.request.retryNumber) iterate(tryNumber + 1)
    }
    iterate(0)
  }

  private[this] def attemptPerform(responseBuilder: HttpClient#ResponseBuilder): Try[_] = {
    val request = responseBuilder.request
    val url = new URL(request.url.toString)
    val connection = Try(url.openConnection().asInstanceOf[HttpURLConnection])
    if (connection.isFailure)
      return connection

    implicit val buffer = this.buffer
    val result: Try[_] = Try {
      configureConnection(connection.get, request)
      writeBody(connection.get, request)

      responseBuilder.statusCode = connection.get.getResponseCode
      responseBuilder.statusMessage = connection.get.getResponseMessage
      responseBuilder.body = readBody(connection.get)
      val headers = mutable.Map[String, String]()
      for (pos <- 0 until connection.get.getHeaderFields.size()) {
        headers(connection.get.getHeaderFieldKey(pos)) = connection.get.getHeaderField(pos)
      }
      responseBuilder.headers = headers.toMap
      responseBuilder.bodyEncoding = connection.get.getContentEncoding
      0
    }
    connection.get.disconnect()
    result
  }

  private[this] def configureConnection(connection: HttpURLConnection,
                                        request: HttpClient#Request): Unit = {

    request.headers.foreach {case (k, v) =>
      connection.setRequestProperty(k, v)
    }
    connection.setUseCaches(request.cached)
    connection.setConnectTimeout(request.connectTimeout)
    connection.setReadTimeout(request.readTimeout * 5000)
    connection.setDoInput(true)
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
                             (implicit buffer: Array[Char]): Unit = {
    request.method match {
      case "POST" | "PUT" =>
        if (request.body.markSupported())
          request.body.mark(Int.MaxValue)
        val out = new BufferedOutputStream(connection.getOutputStream)
        val in = new BufferedInputStream(request.body)
        IOUtils.copy(in, out)
        out.close()
        // Don't close the input stream in case of error
      case nothing => // Nothing to do
    }
  }

  private def readBody(connection: HttpURLConnection): InputStream = {
    val output = new ByteArrayOutputStream()
    val input = {
      if (connection.getResponseCode >= 200 && connection.getResponseCode <= 299)
        connection.getInputStream
      else
        connection.getErrorStream
    }
    var length = 0
    val buffer = new Array[Byte](4096)
    while ({length = input.read(buffer); length != -1}) {
      output.write(buffer, 0, length)
    }
    input.close()
    new ByteArrayInputStream(output.toByteArray)
  }

  private[this] def buffer: Array[Char] = {
    synchronized {
      var buffer = Buffers.get(Thread.currentThread().getId)
      if (buffer.isEmpty) {
        buffer = Option(new Array[Char](BufferSize))
        Buffers(Thread.currentThread().getId) = buffer.get
      }
      buffer.get
    }
  }

}
