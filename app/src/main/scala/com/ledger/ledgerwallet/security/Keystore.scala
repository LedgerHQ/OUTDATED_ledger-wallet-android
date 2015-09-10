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
package com.ledger.ledgerwallet.security

import java.io.{FileOutputStream, File}
import java.security.{Provider, KeyStore}
import java.security.KeyStore.{Entry, ProtectionParameter, PasswordProtection, PrivateKeyEntry}

import android.content.Context
import android.preference.PreferenceManager
import com.ledger.ledgerwallet.security.Keystore.{KeystoreNotInstalledException, LockedKeystoreException}
import scala.util.Try


class Keystore(c: Context) {

  private type JavaKeystore = java.security.KeyStore
  private val JavaKeystore = java.security.KeyStore

  private[this] var _keystore: Option[JavaKeystore] = None
  private[this] lazy val _preferences = PreferenceManager.getDefaultSharedPreferences(c)
  private[this] var _keystorePassword: Option[String] = None

  private[this] var _internalKeystoreFileOutput: Option[FileOutputStream] = None

  def unlock(password: String): Boolean = {
    _keystorePassword = Option(password)
    Try(getInternalKeystore).isSuccess
  }

  def isLocked = Try(_keystore).failed.map({case e: LockedKeystoreException => null}).isSuccess
  def isOpen = _keystore.isDefined


  def containsAlias(alias: String): Try[Boolean] = Try {
    keystore.containsAlias(alias)
  }

  def getEntry(alias: String, protectionParameter: ProtectionParameter): Try[Entry] = Try {
    keystore.getEntry(alias, protectionParameter)
  }

  def getProvider: Try[Provider] = Try {
    keystore.getProvider
  }

  def deleteEntry(alias: String): Try[Unit] = Try {
    keystore.deleteEntry(alias)
  }

  /// Reset the currently used keystore
  def reset(): Unit = {
    _internalKeystoreFileOutput.foreach(_.close())
    _internalKeystoreFileOutput = None
    _keystorePassword = None
    _keystore = None
  }

  def installInternalKeystore(password: String): Unit = {
    val keystore = JavaKeystore.getInstance(JavaKeystore.getDefaultType)
    if (_keystoreFile.exists()) {
      throw new IllegalStateException("Keystore file already installed")
    }
    keystore.load(null, null)
    keystore.store(c.openFileOutput(_keystoreFile.getName, Context.MODE_PRIVATE), password.toCharArray)
    _keystore = Option(keystore)
  }

  def changeInternalKeystorePassword(oldPassword: String, newPassword: String): Unit = {

    reset()
  }

  def deleteInternalKeystore: Unit = {
    _keystoreFile.delete()
    _keystore = None
  }

  private[this] def keystore: JavaKeystore = {
    _keystore.getOrElse {
      if (_preferences.getBoolean(Keystore.Preferences.UseLocalKeystore, false)) {
        _keystore = Option(getInternalKeystore)
      } else {
        _keystore = Option(KeyStore.getInstance("AndroidKeyStore"))
        _keystore.get.load(null)
      }
      _keystore.get
    }
  }

  private[this] def getInternalKeystore: java.security.KeyStore = {
    val keyStoreFile = new File(c.getFilesDir, "internal.keystore")
    if (!keyStoreFile.exists()) {
      throw new KeystoreNotInstalledException()
    }
    val keystore = JavaKeystore.getInstance(JavaKeystore.getDefaultType)
    _internalKeystoreFileOutput = Option(c.openFileOutput(_keystoreFile.getName, Context.MODE_PRIVATE))
    val input = c.openFileInput(_keystoreFile.getName)
    keystore.load(input, _keystorePassword.getOrElse("").toCharArray)
    input.close()
    keystore
  }

  private[this] def _keystoreFile = new File(c.getFilesDir, "internal.keystore")

  private[this] def _store(): Unit = {
    _internalKeystoreFileOutput.foreach(_keystore.get.store(_, _keystorePassword.get.toCharArray))
  }

}

object Keystore {

  private[this] var _defaultInstance: Option[Keystore] = None

  def defaultInstance(implicit context: Context) = {
    _defaultInstance.getOrElse {
      _defaultInstance = Option(new Keystore(context.getApplicationContext))
      _defaultInstance.get
    }
  }

  object Preferences {
    val UseLocalKeystore = "UseLocalKeystore"
  }

  sealed class KeystoreNotInstalledException extends Exception("The keystore needs to be installed")
  sealed class LockedKeystoreException extends Exception("The keystore is presently closed")

}