/**
 *
 * PrivateKey
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 06/02/15.
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
package co.ledger.wallet.crypto

import java.security.KeyPair
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

import android.content.Context
import co.ledger.wallet.security.Keystore
import co.ledger.wallet.utils.logs.Logger
import org.spongycastle.util.encoders.Hex

import scala.util.Try

sealed trait SecretKey {

  def alias: String
  def raw: Array[Byte]
  def secret: Array[Byte]

}

object SecretKey {

  private type JavaKeystore = java.security.KeyStore
  private type JavaKeyPair = java.security.KeyPair
  private type JavaKeyGenerator = java.security.KeyPairGenerator
  private type PrivateKeyEntry = java.security.KeyStore.PrivateKeyEntry

  private[this] implicit var context: Context = null;

  private[this] lazy val keystore: Keystore = {
    val keystore = Keystore.defaultInstance
    keystore.load(null)
    keystore
  }

  def get(context: Context, alias: String): Option[SecretKey] = {
    this.context = context.getApplicationContext
    val store = keystore
    val hexWrappedSecret = context.getSharedPreferences(PreferenceName, Context.MODE_PRIVATE).getString(alias, null)
    if (!store.containsAlias(alias) || hexWrappedSecret == null)
      return None
    val raw = Hex.decode(hexWrappedSecret)
    val entry = keystore.getEntry(alias, null)
    entry match {
      case privateKeyEntry: PrivateKeyEntry =>
        Some(new SecretKeyImpl(alias, raw, new KeyPair(privateKeyEntry.getCertificate.getPublicKey, privateKeyEntry.getPrivateKey)))
      case wrongEntryClass => None
    }
  }

  def remove(context: Context, alias: String): Boolean = {
    this.context = context.getApplicationContext
    val store = keystore
    if (store.containsAlias(alias)) {
      store.deleteEntry(alias)
      true
    } else {
      false
    }
  }

  def create(context: Context, alias: String, secret: Array[Byte]): Try[SecretKey] = {
    this.context = context.getApplicationContext
    keystore.generateKey(alias)
    Logger.d("After generate keypair")
    val entry = keystore.getEntry(alias, null)
    entry match {
      case privateKeyEntry: PrivateKeyEntry =>
        val kp = new KeyPair(privateKeyEntry.getCertificate.getPublicKey, privateKeyEntry.getPrivateKey)
        val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")
        cipher.init(Cipher.WRAP_MODE, kp.getPublic)
        val wrappedSecret = cipher.wrap(new SecretKeySpec(secret, "AES"))
        context.getSharedPreferences(PreferenceName, Context.MODE_PRIVATE)
          .edit()
          .putString(alias, Hex.toHexString(wrappedSecret))
          .commit()
        Try(new SecretKeyImpl(alias, wrappedSecret, kp))
      case unsupported => Try(throw new Exception("Unable to create certificates"))
    }
  }

  def delete(context: Context, alias: String): Unit = {
    keystore.deleteEntry(alias)
    context.getSharedPreferences(PreferenceName, Context.MODE_PRIVATE)
      .edit()
      .remove(alias)
      .commit()
  }

  private class SecretKeyImpl(_alias: String, _raw: Array[Byte], keyPair: KeyPair) extends SecretKey {
    private[this] val cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding")

    override def alias: String = _alias
    override def raw: Array[Byte] = _raw
    override def secret: Array[Byte] = {
      cipher.init(Cipher.UNWRAP_MODE, keyPair.getPrivate)
      cipher.unwrap(_raw, "AES", Cipher.SECRET_KEY).getEncoded
    }
  }

  private val PreferenceName = "SecretKeyStore"

}