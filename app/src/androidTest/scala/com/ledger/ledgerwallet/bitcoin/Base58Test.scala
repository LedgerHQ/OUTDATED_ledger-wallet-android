/**
 *
 * Base58
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/02/15.
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

import android.test.InstrumentationTestCase
import com.ledger.ledgerwallet.utils.logs.Logger
import junit.framework.Assert
import org.spongycastle.util.encoders.Hex

class Base58Test extends InstrumentationTestCase {

  // https://github.com/bitcoin/bitcoin/blob/master/src/test/base58_tests.cpp
  val tuples = Array(
    ("", Array[Byte]()),
    ("1112", Array[Byte](0x00, 0x00, 0x00, 0x01)),
    ("2g", Array[Byte](0x61)),
    ("a3gV", Array[Byte](0x62,0x62,0x62)),
    ("aPEr", Array[Byte](0x63,0x63,0x63)),
    ("2cFupjhnEsSn59qHXstmK2ffpLv2", Array[Byte](0x73,0x69,0x6d,0x70,0x6c,0x79,0x20,0x61,0x20,0x6c,0x6f,0x6e,0x67,0x20,0x73,0x74,0x72,0x69,0x6e,0x67)),
    ("1NS17iag9jJgTHD1VXjvLCEnZuQ3rJDE9L", Hex.decode("00eb15231dfceb60925886b67d065299925915aeb172c06647")),
    ("ABnLTmg", Hex.decode("516b6fcd0F")),
    ("3SEo3LWLoPntC", Hex.decode("bf4f89001e670274dd")),
    ("3EFU7m", Hex.decode("572e4794")),
    ("EJDM8drfXA6uyA", Hex.decode("ecac89cad93923c02321")),
    ("Rt5zm", Hex.decode("10C8511E")),
    ("1111111111", Array[Byte](0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00,0x00))
  )

  def testEncode(): Unit = {
      tuples foreach {case (encoded, decoded) =>
        Logger.d(encoded + " <> " + Base58.encode(decoded))
        Assert.assertEquals(encoded, Base58.encode(decoded))
      }
  }

  def testDecode(): Unit = {
    tuples foreach {case (encoded, decoded) =>
      Assert.assertEquals(decoded.deep, Base58.decode(encoded).deep)
    }
  }

  val bitcoinAddress = ("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvM", Hex.decode("00010966776006953D5567439E5E39F86A0D273BEE"))

  def testEncodeBitcoinAddress(): Unit = {
    Assert.assertEquals(bitcoinAddress._1, Base58.encodeWitchChecksum(bitcoinAddress._2))
  }

  def testDecodeBitcoinAddress(): Unit = {
    Assert.assertEquals(bitcoinAddress._2.deep, Base58.checkAndDecode(bitcoinAddress._1).getOrElse(Array[Byte]()).deep)
  }

  def testDecodeInvalidBitcoinAddress(): Unit = {
    Assert.assertEquals(null, Base58.checkAndDecode("16UwLL9Risc3QfPqBUvKofHmBQ7wMtjvA").orNull)
  }

}
