/**
 *
 * Keystore
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/09/15.
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

import java.io.{FileOutputStream, File}
import java.security.{Provider, KeyStore}
import java.security.KeyStore.{Entry, ProtectionParameter, PasswordProtection, PrivateKeyEntry}

import android.content.Context
import android.preference.PreferenceManager
import co.ledger.wallet.core.security.Keystore.{KeystoreNotInstalledException, LockedKeystoreException}
import scala.concurrent.{Promise, Future}
import scala.util.{Failure, Success, Try}
import scala.concurrent.ExecutionContext.Implicits.global

abstract class Keystore(c: Context) {

  type JavaKeyStore = java.security.KeyStore
  type JavaKeyPair = java.security.KeyPair

  def containsAlias(alias: String): Boolean = {
    ensureKeyStoreIsLoaded
    _javaKeystore.get.containsAlias(alias)
  }

  def aliases: Iterator[String] = {
    ensureKeyStoreIsLoaded
    val it = _javaKeystore.get.aliases()
    new Iterator[String]() {
      def next = it.nextElement()
      def hasNext = it.hasMoreElements
    }
  }

  def safe[A](context: Context)(closure: => A): Future[A] = {
    val promise = Promise[A]()
    val result = Try(closure)
    if (result.isFailure && result.failed.get.isInstanceOf[AssertionError] && _javaKeystore.isEmpty) {
      load(null).onComplete {
        case Success(keystore) => promise.complete(Try(closure))
        case Failure(_) => promptUnlockDialog(context).onComplete {
          case Success(keystore) => promise.complete(Try(closure))
          case Failure(ex) => promise.failure(ex)
        }
      }
    } else {
      promise.complete(result)
    }
    promise.future
  }

  def deleteEntry(alias: String): Unit = {
    ensureKeyStoreIsLoaded
    _javaKeystore.get.deleteEntry(alias)
  }

  def getEntry(alias: String, protectionParameter: ProtectionParameter): KeyStore.Entry = {
    ensureKeyStoreIsLoaded
    _javaKeystore.get.getEntry(alias, protectionParameter)
  }

  def generateKey(alias: String): JavaKeyPair

  def load(passwordProtection: PasswordProtection): Future[Keystore] = {
    if (_javaKeystore.isDefined) {
      return Promise().success(this).future
    }
    loadJavaKeyStore(passwordProtection).map((keystore) => {
      _javaKeystore = Option(keystore)
      this
    })
  }

  def unload(): Unit = {
    _javaKeystore = None
  }

  protected def loadJavaKeyStore(passwordProtection: PasswordProtection): Future[JavaKeyStore]

  def promptUnlockDialog(context: Context): Future[Keystore] = {
    Promise()
      .failure(
        new UnsupportedOperationException("promptUnlockDialog is not supported by this keystore")
      ).future
  }

  def isLoaded = _javaKeystore.isDefined
  private[this] var _javaKeystore: Option[JavaKeyStore] = None
  protected def javaKeystore = _javaKeystore
  private[this] var _loadedKeystore: Future[KeyStore] = null

  @inline private[this] def ensureKeyStoreIsLoaded = assert(isLoaded, "Keystore is locked")

}

object Keystore {

  type Mode = Int

  val InternalMode = 0x01
  val AndroidMode = 0x02
  val PreferenceMode = 0x03


  private[this] var _defaultInstance: Option[Keystore] = None

  def defaultInstance(implicit context: Context) = {
    _defaultInstance.getOrElse({
      _defaultInstance = Option(new AndroidKeystore(context))
      _defaultInstance.get
    })
  }

  object Preferences {
    val UseInternalKeystore = "use_internal_keystore"
  }

  sealed class KeystoreNotInstalledException extends Exception("The keystore needs to be installed")
  sealed class LockedKeystoreException extends Exception("The keystore is presently closed")

}