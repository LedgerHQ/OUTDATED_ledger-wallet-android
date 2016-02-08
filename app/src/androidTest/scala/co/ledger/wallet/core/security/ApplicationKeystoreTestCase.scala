/**
 *
 * ApplicationKeystoreTestCase
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 08/02/16.
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
package co.ledger.wallet.core.security

import java.security.KeyStore.PasswordProtection

import android.content.Context
import co.ledger.wallet.InstrumentationTestCase
import co.ledger.wallet.core.crypto.SecretKey
import co.ledger.wallet.core.utils.HexUtils
import co.ledger.wallet.core.utils.logs.Logger

import scala.concurrent.Await
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._

class ApplicationKeystoreTestCase extends InstrumentationTestCase {

  val MySecretKey = "000102030405060708090a0b0c0d0e0f"

  var keystore: ApplicationKeystore = _
  def context: Context = getInstrumentation.getContext
  override def setUp(): Unit = {
    super.setUp()
    keystore = new ApplicationKeystore(getInstrumentation.getTargetContext, "test")
  }

  def testSetAndGet(): Unit = {
    val f = keystore.install(new PasswordProtection("astrongpassword".toCharArray)) map {(keystore) =>
      val key = SecretKey.create(context, keystore, "TheAlias", HexUtils.decodeHex(MySecretKey))
      key.get
    } flatMap {(key) =>
      SecretKey.get(context, keystore, "TheAlias").map(_ -> key)
    } map {
      case (getKey, genKey) =>
        Logger.d(s"Got secrets ${HexUtils.bytesToHex(genKey.secret)} == ${HexUtils.bytesToHex(getKey.secret)}")()
        if (!(getKey.secret sameElements genKey.secret))
          throw new Exception("Invalid key")
        SecretKey.delete(context, keystore, "TheAlias")
        Logger.d(s"Got secrets ${HexUtils.bytesToHex(genKey.secret)} == ${HexUtils.bytesToHex(getKey.secret)}")
        true
    }
    Await.result(f, 30 seconds)
  }

  override def tearDown(): Unit = {
    super.tearDown()
    keystore.delete()
  }
}
