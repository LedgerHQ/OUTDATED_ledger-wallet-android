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

import android.content.Intent
import android.os.Bundle
import android.text.method.ScrollingMovementMethod
import android.widget.{ProgressBar, TextView}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{BaseActivity, WalletActivity}
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.wallet.events.PeerGroupEvents.BlockDownloaded
import co.ledger.wallet.wallet.events.WalletEvents.TransactionReceived

class DemoActivity extends BaseActivity with WalletActivity {

  lazy val text = TR(R.id.text).as[TextView]
  lazy val progress = TR(R.id.progressBar).as[ProgressBar]
  lazy val text2 = TR(R.id.text2).as[TextView]
  var maxBlockLeft = -1

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    Logger.d("UI initialized")
    setContentView(R.layout.demo_activity)
    text.setText("onCreate")
    text.setMovementMethod(new ScrollingMovementMethod)
    //wallet.synchronize().map({(_) => updateTransactionList()})
    //updateTransactionList()
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

  override def receive: Receive = {
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
