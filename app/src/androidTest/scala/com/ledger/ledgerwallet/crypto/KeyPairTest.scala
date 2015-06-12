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
import org.spongycastle.util.encoders.Hex

class KeyPairTest extends InstrumentationTestCase {

  def testShouldGenerateKeypair(): Unit = {
    val keypair = ECKeyPair.generate()
    Logger.d("Public: " + keypair.publicKeyHexString)
    Logger.d("Private: " + keypair.privateKeyHexString)
    Logger.d("Public size: " + (keypair.publicKeyHexString.length / 2))
    Logger.d("Private size: " + (keypair.privateKeyHexString.length / 2))
    Assert.assertEquals(65, keypair.publicKey.length)
    Assert.assertTrue(32 >= keypair.privateKey.length)
  }

  def testShouldPerformECDH(): Unit = {
    val key = ECKeyPair.create(Hex.decode("dbd39adafe3a007706e61a17e0c56849146cfe95849afef7ede15a43a1984491"))
    val otherKey = ECKeyPair.generate()
    Logger.d("My Public key: " + key.publicKeyHexString)
    Logger.d("My Public key size: " + (key.publicKeyHexString.length / 2))
    Logger.d("Other Public key: " + otherKey.publicKeyHexString)
    Logger.d("Other Public key size: " + (otherKey.publicKeyHexString.length / 2))
    val secret = key.generateAgreementSecret(otherKey.publicKeyHexString)
    Logger.d("Secret: " + Hex.toHexString(secret))
    Logger.d("Secret size: " + secret.length)
    val otherSecret = otherKey.generateAgreementSecret(key.publicKeyHexString)
    Logger.d("Other Secret: " + Hex.toHexString(secret))
    Logger.d("Other Secret size: " + secret.length)
    Assert.assertEquals(Hex.toHexString(secret), Hex.toHexString(otherSecret))
  }

  def testShouldPerformECDHWithDeterministKey(): Unit = {
    val key = ECKeyPair.create(Hex.decode("dbd39adafe3a007706e61a17e0c56849146cfe95849afef7ede15a43a1984491"))
    val otherKey = "04ae218d8080c7b9cd141b06f6b9f63ef3adf7aecdf49bb3916ac7f5d887fc4027bea6fd187b9fa810b6d251e1430f6555edd2d5b19828d51908917c03e3f7c436"
    Logger.d("My Public key: " + key.publicKeyHexString)
    Logger.d("My Public key size: " + (key.publicKeyHexString.length / 2))
    Logger.d("Other Public key: " + otherKey)
    Logger.d("Other Public key size: " + (otherKey.length / 2))
    val secret = key.generateAgreementSecret(otherKey)
    Logger.d("Secret: " + Hex.toHexString(secret))
    Logger.d("Secret size: " + secret.length)
    Assert.assertEquals(Hex.toHexString(secret), "ee0eb1f6dc57e36f95a3bc750d3b798c61c79870eefd7989dc27ec5f3f77d2ec")
  }

  def testShouldPerformECDHWithPrivateKeys(): Unit = {
    val key = ECKeyPair.create(Hex.decode("E34B1842CD2C8134EB172EAB319F73A41D0CAF4E4FEA33CB0B6DCE05D208ADD1"))
    val otherKey = ECKeyPair.create(Hex.decode("C6A8046E163EFD1F74144A48BD2016BDE91E53D4B60E9A55691F6D75CC11A10A"))
    Logger.d("My Public key: " + key.publicKeyHexString)
    Logger.d("My Public key size: " + (key.publicKeyHexString.length / 2))
    Logger.d("Other Public key: " + otherKey.publicKeyHexString)
    Logger.d("Other Public key size: " + (otherKey.publicKeyHexString.length / 2))
    val secret = key.generateAgreementSecret(otherKey.publicKeyHexString)
    Logger.d("Secret: " + Hex.toHexString(secret))
    Logger.d("Secret size: " + secret.length)
    val otherSecret = otherKey.generateAgreementSecret(key.publicKeyHexString)
    Logger.d("Other Secret: " + Hex.toHexString(secret))
    Logger.d("Other Secret size: " + secret.length)
    Assert.assertEquals(Hex.toHexString(secret), Hex.toHexString(otherSecret))
  }

}
