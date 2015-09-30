/**
 *
 * PairingInProgressFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 02/02/15.
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
package co.ledger.wallet.app.m2fa.pairing

import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.ProgressBar
import co.ledger.wallet.R
import co.ledger.wallet.base.{ContractFragment, BaseFragment}
import co.ledger.wallet.utils.TR
import co.ledger.wallet.widget.TextView

class PairingInProgressFragment extends BaseFragment with ContractFragment[CreateDonglePairingActivity.CreateDonglePairingProccessContract] {

  private val ExtraTitleId = "ExtraTitleId"
  private val ExtraTextId = "ExtraTextId"
  private val ExtraStep = "ExtraStep"

  lazy val titleView = TR(R.id.title).as[TextView]
  lazy val textView = TR(R.id.text).as[TextView]
  lazy val loader = TR(R.id.progress).as[ProgressBar]

  def this(step: Int, title: Int, text: Int) = {
    this()
    val args = new Bundle()
    args.putInt(ExtraTitleId, title)
    args.putInt(ExtraTextId, text)
    args.putInt(ExtraStep, step)
    setArguments(args)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = inflater.inflate(R.layout.pairing_in_progress_fragment, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    titleView.setText(getArguments.getInt(ExtraTitleId))
    textView.setText(getArguments.getInt(ExtraTextId))
    // Sets the progress drawable
  }

  override def tag: String = "PairingInProgressFragment_" + getArguments.getInt(ExtraStep, 0)
}