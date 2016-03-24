/**
 *
 * IOUtils
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 30/06/15.
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
package co.ledger.wallet.core.utils.io

import java.io._

import co.ledger.wallet.core.utils.logs.Logger

import scala.annotation.tailrec

object IOUtils {

  val BufferSize = 8192

  def copy(source: InputStream,
           destination: OutputStream,
           buffer: Array[Char],
           progress: (Long) => Unit)
  : Unit = {
    copy(new InputStreamReader(source), new OutputStreamWriter(destination), buffer, progress)
  }

  def copy(source: InputStream,
           destination: OutputStream,
           buffer: Array[Char])
  : Unit = {
    copy(new InputStreamReader(source), new OutputStreamWriter(destination), buffer)
  }

  def copy(source: InputStream,
           destination: OutputStream)
  : Unit = {
    copy(new InputStreamReader(source), new OutputStreamWriter(destination))
  }

  @tailrec
  def copy(source: Reader,
           destination: Writer,
           buffer: Array[Char] = new Array(BufferSize),
           progress: (Long) => Unit = _ => ()): Unit = {
    require(source != null)
    require(destination != null)

    var read: Int = 0
    read = source.read(buffer)
    if (read == -1)
      return
    destination.write(buffer, 0, read)
    progress(read)
    copy(source, destination, buffer, progress)
  }

  def copy(source: Reader,
           destination: File): Unit = {
    require(source != null)
    require(destination != null)
    val output = new FileWriter(destination)
    copy(source, output)
    output.close()
  }

  def copy(source: File,
           destination: Writer): Unit = {
    require(source != null)
    require(destination != null)
    val input = new FileReader(source)
    copy(input, destination)
    input.close()
  }



}
