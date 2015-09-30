/**
 *
 * AndroidKeystore
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/09/15.
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
package co.ledger.wallet.security

import java.math.BigInteger
import java.security.KeyStore
import java.security.KeyStore.PasswordProtection
import java.util.Calendar
import javax.security.auth.x500.X500Principal

import android.content.Context
import android.security.KeyPairGeneratorSpec
import co.ledger.wallet.crypto.Crypto

import scala.concurrent.{Promise, Future}

class AndroidKeystore(context: Context) extends Keystore(context) {

  override protected def loadJavaKeyStore(passwordProtection: PasswordProtection): Future[JavaKeyStore] = {
    val keystore = KeyStore.getInstance("AndroidKeyStore")
    keystore.load(null)
    Future.successful(keystore)
  }

  override def generateKey(alias: String): JavaKeyPair = {
    Crypto.ensureSpongyIsRemoved()
    val kpg = java.security.KeyPairGenerator.getInstance("RSA", "AndroidKeyStore")
    val calendar = Calendar.getInstance()
    val now = calendar.getTime
    calendar.add(Calendar.YEAR, 100)
    val end = calendar.getTime
    kpg.initialize(
      new KeyPairGeneratorSpec.Builder(context.getApplicationContext)
        .setAlias(alias)
        .setStartDate(now)
        .setEndDate(end)
        .setSerialNumber(BigInteger.valueOf(1))
        .setSubject(new X500Principal("CN=Ledger"))
        .build()
    )
    kpg.generateKeyPair()
  }
}
