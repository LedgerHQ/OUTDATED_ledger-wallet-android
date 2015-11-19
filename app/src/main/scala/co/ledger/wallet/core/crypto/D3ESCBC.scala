/**
 *
 * D3ESCBC
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 05/02/15.
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
package co.ledger.wallet.core.crypto

import javax.crypto.Cipher
import javax.crypto.spec.{SecretKeySpec, IvParameterSpec}

class D3ESCBC(secret: Array[Byte], IV: Option[Array[Byte]] = None) {
  Crypto.ensureSpongyIsInserted()

  if (IV.isDefined && IV.get.length != 8) {
    throw new IllegalArgumentException("Initialization Vector must be a 8 bytes array")
  }
  private[this] val iv = new IvParameterSpec(IV.getOrElse(Array[Byte](0, 0, 0, 0, 0, 0, 0, 0)))
  private[this] val cipher = Cipher.getInstance("DESede/CBC/NoPadding", "BC")
  private[this] val secretKey = new SecretKeySpec(secret, "DESede")

  def encrypt(byte: Array[Byte]): Array[Byte] = {
    Crypto.ensureSpongyIsInserted()
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, iv)
    cipher.doFinal(byte)
  }

  def decrypt(byte: Array[Byte]): Array[Byte] = {
    Crypto.ensureSpongyIsInserted()
    cipher.init(Cipher.DECRYPT_MODE, secretKey, iv)
    cipher.doFinal(byte)
  }

}
