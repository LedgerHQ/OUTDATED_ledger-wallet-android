/**
 *
 * IncomingTransactionDialogFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 22/01/15.
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
package com.ledger.ledgerwallet.app.m2fa

import java.text.SimpleDateFormat
import java.util.Locale

import android.content.DialogInterface
import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.BaseDialogFragment
import com.ledger.ledgerwallet.bitcoin.AmountFormatter
import com.ledger.ledgerwallet.remote.api.m2fa.IncomingTransactionAPI
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.view.DialogActionBarController
import com.ledger.ledgerwallet.widget.TextView

class IncomingTransactionDialogFragment extends BaseDialogFragment {

  lazy val actions = DialogActionBarController(R.id.dialog_action_bar).noNeutralButton
  lazy val amount = TR(R.id.amount).as[TextView]
  lazy val address = TR(R.id.address).as[TextView]
  lazy val date = TR(R.id.date).as[TextView]
  lazy val name = TR(R.id.dongle_name).as[TextView]

  private[this] var _transaction: Option[IncomingTransactionAPI#IncomingTransaction] = None

  def this(tx: IncomingTransactionAPI#IncomingTransaction) {
    this()
    _transaction = Option(tx)
    setCancelable(false)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.incoming_transaction_dialog_fragment, container, false)
  }

  override def onResume(): Unit = {
    super.onResume()
    if (_transaction.isEmpty || _transaction.get.isDone)
      dismiss()
    _transaction.foreach(_.onCancelled(dismiss))
  }

  override def onPause(): Unit = {
    super.onPause()
    _transaction.foreach(_.onCancelled(null))
    dismissAllowingStateLoss()
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    actions onPositiveClick {
      _transaction.foreach(_.accept())
      _transaction = None
      dismiss()
    }
    actions onNegativeClick {
      _transaction.foreach(_.reject())
      _transaction = None
      dismiss()
    }
    _transaction match {
      case Some(transaction) =>
        amount.setText(AmountFormatter.Bitcoin.format(transaction.amount))
        address.setText(transaction.address)
        name.setText(transaction.dongle.name.get)
        val df = android.text.format.DateFormat.getDateFormat(getActivity)
        val hf = android.text.format.DateFormat.getTimeFormat(getActivity)
        date.setText(TR(R.string.incoming_tx_date).as[String].format(df.format(transaction.date), hf.format(transaction.date)))
      case _ =>
    }
  }

  override def onDismiss(dialog: DialogInterface): Unit = {
    super.onDismiss(dialog)
    _transaction.foreach(_.cancel())
  }
}

object IncomingTransactionDialogFragment {
  val DefaultTag = "IncomingTransactionDialogFragment"

}