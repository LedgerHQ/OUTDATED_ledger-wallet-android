/**
  *
  * WalletPreferences
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 12/04/16.
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
package co.ledger.wallet.app.wallet

import java.io.File
import javax.crypto.{SecretKeyFactory, Cipher}
import javax.crypto.spec.PBEKeySpec

import android.content.Context
import co.ledger.wallet.core.concurrent.DebounceFunction
import co.ledger.wallet.core.crypto.Sha256
import co.ledger.wallet.core.utils.{ProtobufHelper, HexUtils}
import co.ledger.wallet.preferences.WalletPreferencesProtos
import co.ledger.wallet.preferences.WalletPreferencesProtos.WalletPreferences.CustomNode

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

class WalletPreferences(directory: File, password: Option[String])(implicit ec: ExecutionContext) {

  private val _data: WalletPreferencesProtos.WalletPreferences = {
   tryLoadFromFile().recover({
     case all =>
        val d = new WalletPreferencesProtos.WalletPreferences
        d
   }).get
  }

  def synchronizer_=(mode: Int) = set(_data.synchronizer = mode)
  def synchronizer = get(_data.synchronizer)

  def delayBeforeShutdown_=(delay: Long) = set(_data.delayBeforeShutdown = delay)
  def delayBeforeShutdown = get(_data.delayBeforeShutdown)

  def counterpart_=(identifier: String) = set(_data.counterpart = identifier)
  def counterpart = get(_data.synchronizer)

  def amountUnit_=(unit: Int) = set(_data.amountUnit = unit)
  def amountUnit = get(_data.amountUnit)

  def explorerName_=(name: String) = set(_data.explorerName = name)
  def explorerName = get(_data.explorerName)

  def discoveryMode_=(mode: Int) = set(_data.discoveryMode = mode)
  def discoveryMode = get(_data.discoveryMode)

  def customNodes_=(nodes: Array[CustomNode]) = set(_data.customNodes = nodes)
  def addCustomNode(node: CustomNode) = {
    customNodes = _data.customNodes :+ node
  }
  def removeCustomNode(node: CustomNode) = {
    customNodes = _data.customNodes.filter(_ != node)
  }
  def customNodes = get(_data.customNodes)

  def save(): Unit = _save()
  private val _save = DebounceFunction {(unit: Unit) =>
    ProtobufHelper.encryptedAtomicWriteToFile(_data, directory, "wallet_preferences", password.getOrElse(""))
  }

  private def set(handler: => Unit): Unit = synchronized {
    handler
    save()
  }

  private def get[A](handler: => A): A = synchronized(handler)

  private def file = new File(directory, "wallet_preferences")
  private def tryLoadFromFile(): Try[WalletPreferencesProtos.WalletPreferences] = Try {
    null
  }
}