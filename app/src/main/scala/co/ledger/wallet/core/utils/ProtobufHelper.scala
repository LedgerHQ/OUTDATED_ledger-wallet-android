/**
  *
  * ProtobufHelper
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 13/04/16.
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

import java.io.File
import javax.crypto.spec.PBEKeySpec
import javax.crypto.{SecretKeyFactory, Cipher}

import co.ledger.wallet.core.utils.io.IOUtils
import co.ledger.wallet.wallet.api.ApiWalletClientProtos
import com.google.protobuf.nano.{MessageNano, CodedOutputByteBufferNano}

object ProtobufHelper {

  def toByteArray(message: MessageNano): Array[Byte] = {
    val raw = new Array[Byte](message.getSerializedSize)
    val output = CodedOutputByteBufferNano.newInstance(raw)
    message.writeTo(output)
    raw
  }

  def writeToFile(message: MessageNano, file: File): Unit = {
    IOUtils.copy(toByteArray(message), file)
  }

  def atomicWriteToFile(message: MessageNano, directory: File, fileName: String): Unit = {
    val file = new File(directory, fileName)
    val tmpFile = new File(directory, s"______tmp_$fileName")
    writeToFile(message, tmpFile)
    tmpFile.renameTo(file)
  }

  def encryptedAtomicWriteToFile(message: MessageNano,
                                 directory: File,
                                 fileName: String,
                                 password: String): Unit = {
    val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
    val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
    val keySpec = new PBEKeySpec(password.toCharArray)
    val key = factory.generateSecret(keySpec)
    cipher.init(Cipher.ENCRYPT_MODE, key)
    val raw = toByteArray(message)
    val encrypted = cipher.doFinal(raw)
    val file = new File(directory, fileName)
    val tmpFile = new File(directory, s"______tmp_$fileName")
    IOUtils.copy(encrypted, file)
    tmpFile.renameTo(file)
  }

  def parseFrom[A <: MessageNano](message: A, file: File)(onEmptyMessage: (A) => Unit): A = {
    if (file.exists()) {
      val input = IOUtils.copy(file)
      message.mergeFrom(input)
    } else {
     onEmptyMessage(message)
    }
    message
  }

}
