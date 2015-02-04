/**
 *
 * SpongyCastleTest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 04/02/15.
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
package com.ledger.ledgerwallet.crypto

import android.test.InstrumentationTestCase
import com.ledger.ledgerwallet.utils.logs.Logger
import junit.framework.Assert

class SpongyCastleTest extends InstrumentationTestCase {

  def testShouldCompleteEcdhWithSessionKey: Unit = {
    val keypair = ECKeyPair.generate()
    Logger.d("Public: " + keypair.publicKeyHexString)
    Logger.d("Private: " + keypair.privateKeyHexString)
    Logger.d("Public size: " + (keypair.publicKeyHexString.length / 2))
    Logger.d("Private size: " + (keypair.privateKeyHexString.length / 2))
    Assert.assertEquals(65, keypair.publicKey.length)
    Assert.assertEquals(32, keypair.privateKey.length)
  }

}
