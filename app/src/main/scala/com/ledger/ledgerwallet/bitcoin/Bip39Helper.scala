/**
 *
 * Bip39Hekper
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 15/09/15.
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
package com.ledger.ledgerwallet.bitcoin

import java.security.SecureRandom

import com.ledger.ledgerwallet.bitlib.crypto.{RandomSource, Bip39}

object Bip39Helper {

  val EnglishWords = Bip39.ENGLISH_WORD_LIST // Temporary solution before refactoring

  private[this] lazy val _secureRandom = new SecureRandom()

  def generateMnemonicPhrase(dictionary: Array[String] = EnglishWords): String = {
    Bip39.createRandomMasterSeed(new RandomSource() {
      override def nextBytes(bytes: Array[Byte]): Unit = _secureRandom.nextBytes(bytes)
    }).getBip39WordList.toArray.mkString(" ")
  }


}
