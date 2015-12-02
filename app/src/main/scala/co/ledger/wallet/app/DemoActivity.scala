/**
 *
 * DemoActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/11/15.
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
package co.ledger.wallet.app

import java.util.Date

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.{Fragment, FragmentPagerAdapter}
import android.support.v4.view.ViewPager
import android.support.v7.app.AlertDialog
import android.text.method.ScrollingMovementMethod
import android.view.{ViewGroup, LayoutInflater, View}
import android.widget.{Button, ProgressBar, TextView}
import co.ledger.wallet.R
import co.ledger.wallet.common._
import co.ledger.wallet.core.base.{BaseFragment, BaseActivity, UiContext, WalletActivity}
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.wallet.ExtendedPublicKeyProvider
import co.ledger.wallet.wallet.events.PeerGroupEvents._
import co.ledger.wallet.wallet.events.WalletEvents._
import co.ledger.wallet.wallet.exceptions._
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams

import scala.concurrent.Future

class DemoActivity extends BaseActivity with WalletActivity with UiContext {

  val XPubs = Array(
    "xpub6D4waFVPfPCpRvPkQd9A6n65z3hTp6TvkjnBHG5j2MCKytMuadKgfTUHqwRH77GQqCKTTsUXSZzGYxMGpWpJBdYAYVH75x7yMnwJvra1BUJ",
    "xpub6D4waFVPfPCpUjYZexFNXjxusXSa5WrRj2iU8v5U6x2EvVuHaSKuo1zQEJA6Lt9dRcjgM1CSQmyq3tmSj5jCSup6WC24vRrHrBUyZkv5Jem",
    "xpub6D4waFVPfPCpX183njE1zjMayNCAnMHV4D989WsFd8ENDwfcdogPfRXSaA4opz3qoLoyCZCHZy9F7GQQnBxF4nNmZfXKKiokb2ABY8Bi8Jz"
  )

  lazy val viewPager = TR(R.id.viewpager).as[ViewPager]
  lazy val tabLayout = TR(R.id.tabs).as[TabLayout]
  private lazy val viewPagerAdapter = new ViewPagerAdapter

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    Logger.d("UI initialized")
    setContentView(R.layout.demo_activity)
    viewPagerAdapter.addFragment(DemoOverviewFragment())
    viewPagerAdapter.addFragment(DemoAccountFragment(0))
    viewPagerAdapter.addFragment(DemoAccountFragment(1))
    viewPagerAdapter.addFragment(DemoAccountFragment(3))
    viewPager.setAdapter(viewPagerAdapter)
    viewPager.setOffscreenPageLimit(100) // Whew!
    tabLayout.setupWithViewPager(viewPager)
  }


  override def onResume(): Unit = {
    super.onResume()
    wallet.account(0).synchronize().recover {
      case AccountHasNoXpubException(index) =>
        showMissingXpubDialog(index)
      case exception =>
        exception.printStackTrace()
    }
  }

  private class ViewPagerAdapter extends FragmentPagerAdapter(getSupportFragmentManager) {
    var fragments = Array[Fragment with TabTitleHolder]()

    override def getItem(position: Int): Fragment = fragments(position)

    override def getCount: Int = fragments.length

    def addFragment(fragment: Fragment with TabTitleHolder): Unit = {
      fragments = fragments :+ fragment
    }

    override def getPageTitle(position: Int): CharSequence = fragments(position).tabTitle
  }

  private[this] def updateAccounts(): Unit = {
    wallet.accountsCount().map({(count) =>
      //accountCount.setText(count.toString)
    })

    val text = new StringBuilder
    wallet.accounts() flatMap { (accounts) =>
      Future.sequence((for (account <- accounts) yield account.xpub()).toSeq)
    } map { (xpubs) =>
      for (index <- xpubs.indices) {
        text.append(s"Account #$index ${xpubs(index).serializePubB58(MainNetParams.get())}\n")
      }
    } recover {
      case AccountHasNoXpubException(index) =>
        text.append(s"Account #$index has no xpub yet\n")
        showMissingXpubDialog(index)
      case throwable =>
        throwable.printStackTrace()
        //append(s"Failure: ${throwable.toString}")
    } map { _ =>
      //accountsList.setText(text)
    }
  }


  private[this] def showMissingXpubDialog(index: Int): Unit = {
    new AlertDialog.Builder(this)
      .setMessage(s"Account #$index has no xpub yet\nWould you like to provide one?")
      .setPositiveButton("yes", new OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit =
          wallet.account(index).importXpub(new ExtendedPublicKeyProvider {
            override def generateXpub(path: String): Future[DeterministicKey] = {
              Logger.d("GENERATE XPUB")
              Future(DeterministicKey.deserializeB58(XPubs(index), MainNetParams.get()))
            }
          })
      }).setNegativeButton("no", null)
      .show()
  }



  override def receive: Receive = {

    case AccountCreated(index) => updateAccounts()
    case string: String =>
    case drop =>
  }

}

class DemoHomeFragment extends BaseFragment {
  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_home_tab, container, false)
  }
}

class DemoAccountFragment extends BaseFragment with TabTitleHolder {

  val IndexArgsKey = "IndexArgsKey"

  lazy val accountIndex = getArguments.getInt(IndexArgsKey)
  lazy  val accountIndexTextView = TR(R.id.account_index).as[TextView]

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_account_tab, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    accountIndexTextView.setText(s"Account #$accountIndex")
  }

  override def tabTitle: String = s"Account #$accountIndex"
}

trait TabTitleHolder {
  def tabTitle: String
}

object DemoAccountFragment {

  def apply(accountIndex: Int): DemoAccountFragment = {
    val fragment = new DemoAccountFragment
    val arguments = new Bundle()
    arguments.putInt(fragment.IndexArgsKey, accountIndex)
    fragment.setArguments(arguments)
    fragment
  }

}

class DemoOverviewFragment extends BaseFragment with TabTitleHolder with MainThreadEventReceiver with Loggable {

  lazy val lastBlockTimeTextView = TR(R.id.last_block_date).as[TextView]
  //lazy val progress = TR(R.id.progressBar).as[ProgressBar]
  lazy val lastBlockIndexTextView = TR(R.id.last_block_index).as[TextView]
  //lazy val accountCount = TR(R.id.account_count).as[TextView]
  //lazy val accountsList = TR(R.id.accounts).as[TextView]
  lazy val balanceTextView = TR(R.id.balance).as[TextView]

  override def tabTitle: String = "Overview"

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_home_tab, container, false)
  }


  override def onResume(): Unit = {
    super.onResume()
    register(wallet.eventBus)
  }

  override def onPause(): Unit = {
    super.onPause()
    unregister(wallet.eventBus)
  }

  private[this] def updateBalance(): Unit = {
    wallet.balance().map {(b) =>
      balanceTextView.setText(s"Balance: ${b.toFriendlyString}")
    } recover {
      case throwable: Throwable =>
        throwable.printStackTrace()
    }
  }

  override def receive: Receive = {
    case CoinReceived(index, _) => updateBalance()
    case CoinSent(index, _) => updateBalance()
    case AccountUpdated(index) => updateBalance()
    case event =>
  }

  def wallet = getActivity.asInstanceOf[DemoActivity].wallet
}

object DemoOverviewFragment {

  def apply() = new DemoOverviewFragment

}