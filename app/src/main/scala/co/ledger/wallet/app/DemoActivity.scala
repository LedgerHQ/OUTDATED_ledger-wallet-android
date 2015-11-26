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

import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.text.method.ScrollingMovementMethod
import android.view.View
import android.widget.{Button, ProgressBar, TextView}
import co.ledger.wallet.wallet.ExtendedPublicKeyProvider
import co.ledger.wallet.{common, R}
import co.ledger.wallet.core.base.{BaseActivity, WalletActivity}
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.wallet.events.PeerGroupEvents.BlockDownloaded
import co.ledger.wallet.wallet.events.WalletEvents.{AccountCreated, TransactionReceived}
import co.ledger.wallet.wallet.exceptions.AccountHasNoXpubException
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams
import common._

import scala.concurrent.Future

class DemoActivity extends BaseActivity with WalletActivity {

  val XPubs = Array(
    "xpub6D4waFVPfPCpRvPkQd9A6n65z3hTp6TvkjnBHG5j2MCKytMuadKgfTUHqwRH77GQqCKTTsUXSZzGYxMGpWpJBdYAYVH75x7yMnwJvra1BUJ",
    "xpub6D4waFVPfPCpUjYZexFNXjxusXSa5WrRj2iU8v5U6x2EvVuHaSKuo1zQEJA6Lt9dRcjgM1CSQmyq3tmSj5jCSup6WC24vRrHrBUyZkv5Jem",
    "xpub6D4waFVPfPCpX183njE1zjMayNCAnMHV4D989WsFd8ENDwfcdogPfRXSaA4opz3qoLoyCZCHZy9F7GQQnBxF4nNmZfXKKiokb2ABY8Bi8Jz"
  )

  lazy val text = TR(R.id.text).as[TextView]
  lazy val progress = TR(R.id.progressBar).as[ProgressBar]
  lazy val text2 = TR(R.id.text2).as[TextView]
  lazy val accountCount = TR(R.id.account_count).as[TextView]
  lazy val accountsList = TR(R.id.accounts).as[TextView]
  var maxBlockLeft = -1

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    Logger.d("UI initialized")
    setContentView(R.layout.demo_activity)
    text.setText("onCreate")
    text.setMovementMethod(new ScrollingMovementMethod)
    //wallet.synchronize().map({(_) => updateTransactionList()})
    //updateTransactionList()
    updateAccounts()
    TR(R.id.button2).as[Button].setOnClickListener({(view: View) =>

    })
  }

  def append(text: String): Unit = {
    this.text.setText(this.text.getText.toString + "\n" + text)
  }

  private[this] def updateTransactionList(): Unit = {
    wallet.transactions() map { (transactions) =>
      text.setText("")
      for (transaction <- transactions) {
        append(transaction.getHash.toString)
      }
    } recover {
      case exception => append(s"Error: ${exception.getMessage}")
    }
  }

  private[this] def updateAccounts(): Unit = {
    wallet.accountsCount().map({(count) =>
      accountCount.setText(count.toString)
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
      case throwable =>
        throwable.printStackTrace()
        append(s"Failure: ${throwable.toString}")
    } map { _ =>
      accountsList.setText(text)
    }
  }

  override def receive: Receive = {
    case AccountCreated(index) => updateAccounts()
    case TransactionReceived(tx) => append(s"Received ${tx.getHash.toString}")
    case BlockDownloaded(left) =>
      if (maxBlockLeft == -1) {
        maxBlockLeft = left
        progress.setMax(maxBlockLeft)
      }
      val p = (((maxBlockLeft - left).toDouble / maxBlockLeft.toDouble) * 100).toInt
      text2.setText(s"Synchronizing $p%")
      progress.setProgress(maxBlockLeft - left)
    case string: String => append(string)
  }
}
