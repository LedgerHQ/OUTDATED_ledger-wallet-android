/**
 *
 * TagDispatcherActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 14/09/15.
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
package co.ledger.wallet.legacy.unplugged

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.widget.TextView
import co.ledger.wallet.R
import co.ledger.wallet.core.base.BaseActivity
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.dongle.NfcDongle
import nordpol.android.{OnDiscoveredTagListener, TagDispatcher}

import scala.util.{Failure, Success}

trait UnpluggedSetupActivity extends BaseActivity {

  implicit val LogTag = "UnpluggedSetupActivity"

  protected lazy val stepNumberTextView = TR(R.id.step_number).as[TextView]
  protected lazy val stepInstructionTextView = TR(R.id.instruction_text).as[TextView]

  private lazy val _nextExtra = Option(getIntent.getExtras).getOrElse(new Bundle()).clone().asInstanceOf[Bundle]
  private lazy val _dispatcher = TagDispatcher.get(this, new OnDiscoveredTagListener {
    override def tagDiscovered(tag: Tag): Unit = onTagDiscovered(tag)
  })

  protected def dispatcher = _dispatcher

  protected def onTagDiscovered(tag: Tag): Unit = {
    val dongle = new NfcDongle(tag)
    dongle.checkIsLedgerUnplugged() onComplete {
      case Success(isLedgerUnplugged) =>
        if (isLedgerUnplugged) {
          onUnpluggedDiscovered(dongle)
        } else {
          onNotInstalledTagDiscovered()
        }
      case Failure(error) => onDiscoveredTagError(error)
    }
  }

  protected def onNotInstalledTagDiscovered(): Unit = {

  }

  protected def onUnpluggedDiscovered(dongle: NfcDongle): Unit = {

  }

  protected def onDiscoveredTagError(error: Throwable): Unit = {
    Logger.e(error.getMessage)
    error.printStackTrace()
  }

  override def onResume(): Unit = {
    super.onResume()
    dispatcher.enableExclusiveNfc()
  }

  override def onPause(): Unit = {
    super.onPause()
    dispatcher.disableExclusiveNfc()
  }

  override def onClickHome(): Boolean = {
    false
  }

  def startNextActivity[A <: UnpluggedSetupActivity](nextActivityClass: Class[A]): Unit = {
    val intent = new Intent(this, nextActivityClass)
    intent.putExtras(_nextExtra)
    startActivity(intent)
  }

  def isInCreationMode =
    getIntent.getIntExtra(UnpluggedSetupActivity.ExtraSetupMode, UnpluggedSetupActivity.CreateWalletSetupMode) == UnpluggedSetupActivity.CreateWalletSetupMode

  def isInRestoreMode = !isInCreationMode

  def pin = Option(_nextExtra.getString(UnpluggedSetupActivity.ExtraPinCode))
  def pin_= (pinCode: String): Unit = {
    _nextExtra.putString(UnpluggedSetupActivity.ExtraPinCode, pinCode)
  }
  def hasPinSetup = getIntent.getStringExtra(UnpluggedSetupActivity.ExtraPinCode) != null

  def setupMode = _nextExtra.getInt(UnpluggedSetupActivity.ExtraSetupMode, UnpluggedSetupActivity.CreateWalletSetupMode)
  def setupMode_= (setupMode: Int): Unit = _nextExtra.putInt(UnpluggedSetupActivity.ExtraSetupMode, setupMode)

  def mnemonicPhrase = Option(_nextExtra.getString(UnpluggedSetupActivity.ExtraMnemonicPhrase))
  def mnemonicPhrase_= (mnemonicPhrase: String): Unit = _nextExtra.putString(UnpluggedSetupActivity.ExtraMnemonicPhrase, mnemonicPhrase)

  def keycardSeed = Option(_nextExtra.getString(UnpluggedSetupActivity.ExtraKeycardSeed))
  def keycardSeed_= (mnemonicPhrase: String): Unit = _nextExtra.putString(UnpluggedSetupActivity.ExtraKeycardSeed, mnemonicPhrase)

}


object UnpluggedSetupActivity {
  val CreateWalletSetupMode = 0x01
  val RestoreWalletSetupMode = 0x02

  val ExtraSetupMode = "EXTRA_SETUP_MODE"
  val ExtraPinCode = "EXTRA_PIN_CODE"
  val ExtraMnemonicPhrase = "EXTRA_MNEMONIC_PHRASE"
  val ExtraKeycardSeed = "EXTRA_KEYCARD_SEED"
}