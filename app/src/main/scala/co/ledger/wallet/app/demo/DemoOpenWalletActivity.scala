/**
 *
 * DemoOpenWalletActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/16.
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
package co.ledger.wallet.app.demo

import java.util.{Calendar, Date}

import android.os.Bundle
import android.view.View
import android.widget.Toast
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{WalletActivity, BaseActivity}
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.wallet.{DerivationPath, ExtendedPublicKeyProvider}
import co.ledger.wallet.wallet.exceptions._
import org.bitcoinj.crypto.DeterministicKey

import scala.concurrent.Future
import scala.util.{Failure, Success}

class DemoOpenWalletActivity extends BaseActivity with WalletActivity with ViewFinder {

  lazy val title: TextView = R.id.title
  lazy val text: TextView = R.id.text

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate (savedInstanceState)
    setContentView(R.layout.demo_open_wallet_activity)
    openWallet()
  }

  def openWallet(): Unit = {
    wallet.mostRecentBlock() map {(block) =>
      val calendar = Calendar.getInstance()
      calendar.add(Calendar.DAY_OF_YEAR, -7)
      val minSyncDate = calendar.getTime
      if (block.date.before(minSyncDate))
        throw TooOldException()
      // TODO Open wallet
      ()
    } recover {
      case ex: TooOldException => synchronizeWallet()
      case ex: WalletNotSetupException => setupWallet()
    }
  }

  def setupWallet(): Unit = {
    wallet.setup(_keyProvider) onComplete {
      case Success(_) => synchronizeWallet()
      case Failure(ex) =>
        ex.printStackTrace()
        Toast.makeText(this, s"Fatal error ${ex.getMessage}", Toast.LENGTH_LONG).show()
        finish()
    }
  }

  def synchronizeWallet(): Unit = {
    title.setText("Synchronizing your wallet")

  }

  override def receive: Receive = {
    case all =>
  }

  private val _keyProvider = new KeyProvider
  override implicit def viewId2View[V <: View](id: Int): V = findViewById(id).asInstanceOf[V]

  case class TooOldException() extends Exception

  private class KeyProvider extends ExtendedPublicKeyProvider {

    override def generateXpub(path: DerivationPath): Future[DeterministicKey] = {
      null
    }

  }
}
