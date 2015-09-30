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
package co.ledger.wallet.net

import java.io.{BufferedInputStream, BufferedOutputStream}
import java.net.{HttpURLConnection, URL}
import java.util.zip.GZIPOutputStream

import co.ledger.wallet.utils.io.IOUtils
import co.ledger.wallet.utils.logs.Logger

import scala.annotation.tailrec
import scala.collection.mutable
import scala.concurrent.{Future, ExecutionContext}
import scala.io.Source
import scala.util.{Failure, Success, Try}
import co.ledger.wallet.net.HttpRequestExecutor.defaultExecutionContext

class BasicHttpRequestExecutor extends HttpRequestExecutor {

  val TimeoutDuration = 1000L
  val BufferSize = 10 * 1024 // ~= 10KB buffer
  val Buffers: mutable.Map[Long, Array[Byte]] = mutable.Map[Long, Array[Byte]]()

  override def execute(responseBuilder: HttpClient#ResponseBuilder): Unit = Future {
    Logger.d(s"Begin request ${responseBuilder.request.url.toString}")
    @tailrec
    def iterate(tryNumber: Int): Unit = {
      Logger.d(s"Attempt $tryNumber")
      var continue = false
      attemptPerform(responseBuilder) match {
        case Success(_) =>
          Logger.d("Success")
          Try(responseBuilder.request.body.close())
          responseBuilder.build()
        case Failure(cause) =>
          Logger.d("Failure")
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
    Logger.d(s"Perform request on ${url.toString}")
    val connection = Try(url.openConnection().asInstanceOf[HttpURLConnection])

    if (connection.isFailure)
      return connection

    Logger.d("Step -1")
    implicit val buffer = this.buffer
    Logger.d("Step 0")
    val result: Try[_] = Try {
      Logger.d("Step 1")
      configureConnection(connection.get, request)
      Logger.d("Step 2")
      writeBody(connection.get, request)
      Logger.d("Step 3")

      responseBuilder.statusCode = connection.get.getResponseCode
      Logger.d("Step 4")
      responseBuilder.statusMessage = connection.get.getResponseMessage
      Logger.d("Step 5")
      responseBuilder.body = connection.get.getInputStream
      Logger.d("Step 6")
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
    connection.setReadTimeout(request.readTimeout)
    connection.setDoInput(true)
    request.method match {
      case "POST" | "PUT" =>
        connection.setDoOutput(true)
        connection.setRequestMethod(request.method)
        if (request.isBodyStreamed) {
          connection.setChunkedStreamingMode(request.chunkLength)
        } else {
          Logger
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
        Logger.d(s"Write body")
        if (request.body.markSupported())
          request.body.mark(Int.MaxValue)
        val out = new BufferedOutputStream(connection.getOutputStream)
        val in = new BufferedInputStream(request.body)
        IOUtils.copy(in, out, buffer)
        out.close()
        // Don't close the input stream in case of error
      case nothing => // Nothing to do
    }
  }

  private[this] def buffer: Array[Byte] = {
    Logger.d("Buffer begin")
    synchronized {
      Logger.d("Buffer sync")
      var buffer = Buffers.get(Thread.currentThread().getId)
      Logger.d("Buffer sync 1")
      if (buffer.isEmpty) {
        Logger.d("Buffer sync 2")
        buffer = Option(new Array[Byte](BufferSize))
        Logger.d("Buffer sync 3")
        Buffers(Thread.currentThread().getId) = buffer.get
        Logger.d("Buffer sync 4")
      }
      Logger.d("Buffer sync 5")
      buffer.get
    }
  }

}
