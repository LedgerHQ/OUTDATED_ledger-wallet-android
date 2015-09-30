/**
 *
 * ApplicationKeystore
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

import java.io.File
import java.math.BigInteger
import java.security.KeyStore.{PasswordProtection, PrivateKeyEntry}
import java.security.{KeyStore, SecureRandom}
import java.util.Calendar
import javax.security.auth.x500.X500Principal

import android.content.Context
import co.ledger.wallet.crypto.Crypto
import org.spongycastle.x509.X509V3CertificateGenerator

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class ApplicationKeystore(context: Context, val keystoreName: String) extends Keystore(context) {

  def install(passwordProtection: PasswordProtection): Future[Keystore] = {
    assert(!file.exists(), "The keystore already exists")
    val keyStore = KeyStore.getInstance(KeyStore.getDefaultType)
    keyStore.load(null)
    val output = context.openFileOutput(keystoreName, Context.MODE_PRIVATE)
    keyStore.store(output, passwordProtection.getPassword)
    output.close()
    load(passwordProtection)
  }

  def setPassword(password: String): Unit = {
    require(password != null, "Password cannot be null")
    _password = password.toCharArray
    store()
  }

  override protected def loadJavaKeyStore(passwordProtection: PasswordProtection): Future[JavaKeyStore] = {
    Future {
      assert(file.exists(), "The keystore is not installed yet")
      val keystore = KeyStore.getInstance(KeyStore.getDefaultType)
      val input = context.openFileInput(keystoreName)
      keystore.load(input, passwordProtection.getPassword)
      input.close()
      _password = passwordProtection.getPassword
      keystore
    }
  }

  override def generateKey(alias: String): JavaKeyPair = {
    Crypto.ensureSpongyIsRemoved()
    val kpg = java.security.KeyPairGenerator.getInstance("RSA")
    val calendar = Calendar.getInstance()
    val now = calendar.getTime
    calendar.add(Calendar.YEAR, 100)
    val end = calendar.getTime
    kpg.initialize(1024, new SecureRandom())
    val keypair = kpg.generateKeyPair()
    val certGen = new X509V3CertificateGenerator()
    val dnName = new X500Principal("CN=Ledger")
    certGen.setSerialNumber(BigInteger.valueOf(System.currentTimeMillis()))
    certGen.setSubjectDN(dnName)
    certGen.setIssuerDN(dnName)
    certGen.setNotBefore(now)
    certGen.setNotAfter(end)
    certGen.setPublicKey(keypair.getPublic)
    certGen.setSignatureAlgorithm("SHA256WithRSAEncryption")
    val certificate = certGen.generate(keypair.getPrivate)

    javaKeystore.get.setEntry(alias, new PrivateKeyEntry(keypair.getPrivate, Array(certificate)), null)
    store()
    keypair
  }

  private[this] def store(): Unit = {
    file.delete()
    val output = context.openFileOutput(keystoreName, Context.MODE_PRIVATE)
    javaKeystore.get.store(output, _password)
    output.close()
  }

  @inline private[this] def file = new File(context.getFilesDir, keystoreName)
  private var _password: Array[Char] = null
}
