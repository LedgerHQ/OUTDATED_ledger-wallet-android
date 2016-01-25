/**
 *
 * BytesWriter
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/01/16.
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
package co.ledger.wallet.core.utils

class BytesWriter(length: Int) {

  def writeByteArray(array: Array[Byte]): BytesWriter = {
    for (byte <- array)
      writeByte(byte)
    this
  }

  def writeByte(byte: Int): BytesWriter = {
    writeByte(byte.toByte)
  }

  def writeByte(byte: Byte): BytesWriter = {
    _buffer(_offset) = byte
    _offset += 1
    this
  }

  def toByteArray = _buffer

  private[this] var _offset = 0
  private[this] val _buffer: Array[Byte] = new Array[Byte](length)
}
