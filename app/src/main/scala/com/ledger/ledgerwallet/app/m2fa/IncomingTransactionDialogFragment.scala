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

import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.BaseDialogFragment
import com.ledger.ledgerwallet.view.DialogActionBarController

class IncomingTransactionDialogFragment extends BaseDialogFragment {

  lazy val actions = DialogActionBarController(R.id.dialog_action_bar).noNeutralButton

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.incoming_transaction_dialog_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    actions onPositiveClick dismiss
    actions onNegativeClick dismiss
  }
}

object IncomingTransactionDialogFragment {
  val DefaultTag = "IncomingTransactionDialogFragment"
}