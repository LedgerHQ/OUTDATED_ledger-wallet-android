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

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.{Toast, Button, Spinner}
import co.ledger.wallet.{common, R}
import co.ledger.wallet.core.base.{WalletActivity, BaseFragment}
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.{EditText, TextView}
import common._

class DemoWalletSendFragment extends BaseFragment with ViewFinder {

  def accountSpinner: Spinner = R.id.accounts
  def feesSpinner: Spinner = R.id.fees
  def totalAmountTextView: TextView = R.id.total_amount
  def amountEditText: EditText = R.id.amount
  def scanButton: Button = R.id.scan
  def sendButton: Button = R.id.send

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_wallet_send_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    setupUi()

    wallet.accounts() foreach {(accounts) =>

    }

    sendButton onClick onSendClicked
    scanButton onClick onScanClicked
  }

  def onSendClicked(): Unit = {
    Toast.makeText(getActivity, "Send it now", Toast.LENGTH_SHORT).show()
  }

  def onScanClicked(): Unit = {
    Toast.makeText(getActivity, "Scan QR code", Toast.LENGTH_SHORT).show()
  }

  private def setupUi(): Unit = {

  }

  def wallet = getActivity.asInstanceOf[WalletActivity].wallet

  override implicit def viewId2View[V <: View](id: Int): V = getView.findViewById(id)
    .asInstanceOf[V]
}
