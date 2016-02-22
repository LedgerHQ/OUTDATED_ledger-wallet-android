/**
 *
 * WalletActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 23/11/15.
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
package co.ledger.wallet.core.base

import android.app.Activity
import android.os.Bundle
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.service.wallet.WalletService
import co.ledger.wallet.wallet.Wallet
import co.ledger.wallet.wallet.proxy.WalletProxy
import WalletActivity._

import scala.concurrent.ExecutionContext

trait WalletActivity extends Activity with MainThreadEventReceiver {

  implicit val ec: ExecutionContext

  abstract override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    register(wallet.eventBus)
  }


  abstract override def onResume(): Unit = {
    super.onResume()
    wallet.asInstanceOf[WalletProxy].service().foreach(_.notifyActivityResumed())
    wallet.asInstanceOf[WalletProxy].bind()
  }

  override def onPause(): Unit = {
    super.onPause()
    wallet.asInstanceOf[WalletProxy].service().foreach(_.notifyActivityResumed())
    wallet.asInstanceOf[WalletProxy].unbind()
  }

  abstract override def onDestroy(): Unit = {
    super.onDestroy()
  }

  lazy val wallet: Wallet = new WalletProxy(this, activityWalletName.get)

  def activityWalletName = Option(getIntent.getStringExtra(ExtraWalletName)) orElse {
    WalletService.currentWalletName(this)
  }

}

object WalletActivity {
  val ExtraWalletName = "ExtraWalletName"
}