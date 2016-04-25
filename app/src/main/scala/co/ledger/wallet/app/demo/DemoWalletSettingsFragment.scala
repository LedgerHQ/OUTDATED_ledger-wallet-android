/**
 *
 * DemoWalletHomeFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 01/02/16.
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

import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.support.v7.preference.Preference.OnPreferenceClickListener
import android.support.v7.preference.{Preference, PreferenceFragmentCompat}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{UiContext, WalletActivity}
import co.ledger.wallet.service.wallet.WalletService
import co.ledger.wallet.service.wallet.spv.SpvWalletClient
import co.ledger.wallet.wallet.proxy.WalletProxy
import shapeless.Succ

import scala.util.{Failure, Success}

class DemoWalletSettingsFragment extends PreferenceFragmentCompat with UiContext {

  implicit val ec = co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    addPreferencesFromResource(R.xml.demo_wallet_preferences)
    findPreference("close_wallet").setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        Async {
          Thread.sleep(200)
        } onComplete {
          case all =>
            getActivity.stopService(new Intent(getActivity, classOf[WalletService]))
            WalletService.closeCurrentWallet()(getActivity.getApplicationContext)
            getActivity.finish()
            val intent = new Intent(getActivity, classOf[DemoHomeActivity])
            getActivity.startActivity(intent)
        }
        true
      }
    })
    findPreference("resync").setOnPreferenceClickListener(new OnPreferenceClickListener {
      override def onPreferenceClick(preference: Preference): Boolean = {
        resync()
        true
      }
    })
  }

  override def onCreatePreferences(bundle: Bundle, s: String): Unit = {

  }

  def resync(): Unit = {
    val d = ProgressDialog.show(getActivity, "Loading", "Synchronizing database with Blockchain")
    getActivity.asInstanceOf[WalletActivity].wallet.asInstanceOf[WalletProxy].connect() flatMap {w=>
      w.asInstanceOf[SpvWalletClient].resynchronizeDatabaseWithBlockchain()
    } onComplete {
      case Success(_) => d.dismiss()
      case Failure(ex) =>
        ex.printStackTrace()
        d.dismiss()
    }
  }

  override def isDestroyed: Boolean = false
}
