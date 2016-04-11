/**
 *
 * DemoWalletHomeActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 29/01/16.
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

import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.{Fragment, FragmentPagerAdapter}
import android.support.v4.view.ViewPager
import android.view.View
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{DeviceActivity, BaseActivity, WalletActivity}
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.wallet.events.WalletEvents._

class DemoWalletHomeActivity extends BaseActivity
  with WalletActivity
  with DeviceActivity
  with ViewFinder
  with DemoMenuActivity {

  lazy val viewPager: ViewPager = R.id.viewpager
  lazy val tabLayout: TabLayout = R.id.tabs

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.demo_wallet_activity)
    viewPager.setAdapter(new ViewPagerAdapter)
    tabLayout.setupWithViewPager(viewPager)
    setTitle("")
    fetchBalance()
    wallet.synchronize(null)
  }


  override def receive: Receive = {
    //case CoinSent(_, _) => fetchBalance()
    //case CoinReceived(_, _) => fetchBalance()
    //case NewOperation(_, _) => fetchBalance()
    //case OperationChanged(_, _) => fetchBalance()
    case ignore =>
  }

  def fetchBalance(): Unit = {
    wallet.balance() foreach {(balance) =>
      setTitle(balance.toFriendlyString)
    }
  }

  override implicit def viewId2View[V <: View](id: Int): V = findViewById(id).asInstanceOf[V]

  private class ViewPagerAdapter extends FragmentPagerAdapter(getSupportFragmentManager) {

    val fragments = Array(
      "Home" -> new DemoWalletHomeFragment,
      "Send" -> new DemoWalletSendFragment,
      "Receive" -> new DemoWalletReceiveFragment,
      "Settings" -> new DemoWalletSettingsFragment
    )

    override def getItem(position: Int): Fragment = fragments(position)._2

    override def getCount: Int = fragments.length

    override def getPageTitle(position: Int): CharSequence = fragments(position)._1
  }

}
