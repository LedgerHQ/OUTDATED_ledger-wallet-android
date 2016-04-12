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

import android.net.Uri.Builder
import android.os.{Bundle, Handler}
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.AdapterView.{OnItemClickListener, OnItemSelectedListener}
import android.widget._
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{BaseFragment, UiContext, WalletActivity}
import co.ledger.wallet.core.image.QrCodeGenerator
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.TextView
import org.bitcoinj.core.Coin

import scala.util.{Failure, Success, Try}

class DemoWalletReceiveFragment extends BaseFragment
  with ViewFinder
  with UiContext {

  def qrCodeImageView: ImageView = R.id.qr_code
  def addressTextView: TextView = R.id.address
  def accountSpinner: Spinner = R.id.accounts
  def amountEditText: EditText = R.id.amount

  override def onCreateView(inflater: LayoutInflater,
                            container: ViewGroup,
                            savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.demo_wallet_receive_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    setupUi()
  }


  override def onResume(): Unit = {
    super.onResume()
    refreshAddress()
  }

  def setupUi(): Unit = {
    wallet.accounts() foreach {(accounts) =>
      accountSpinner.setAdapter(new ArrayAdapter[String](getActivity, android.R.layout
        .simple_list_item_1, accounts
        .map((a) =>
        s"Account #${a.index}"
        )
      ))
      accountSpinner.setOnItemSelectedListener(new OnItemSelectedListener {
        override def onNothingSelected(parent: AdapterView[_]): Unit = ()

        override def onItemSelected(parent: AdapterView[_], view: View, position: Int, id: Long):
        Unit = {
          _accountIndex = position
          refreshAddress()
        }
      })
      accountSpinner.setOnItemClickListener(new OnItemClickListener {
        override def onItemClick(parent: AdapterView[_], view: View, position: Int, id: Long):
        Unit = {
          _accountIndex = position
          refreshAddress()
        }
      })
      accountSpinner.setSelection(_accountIndex)
    }
    val handler = new Handler()
    var scheduled: Option[Runnable] = None
    amountEditText.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = ()

      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = ()

      override def afterTextChanged(s: Editable): Unit = {
        if (scheduled.isDefined)
          handler.removeCallbacks(scheduled.get)
        val content = s.toString.trim
        scheduled = Some(new Runnable {
          override def run(): Unit = {
            scheduled = None
            if (content.length == 0 || Try(content.toDouble).isFailure)
              _amount = None
            else {
              val bitcoin = content.toDouble * Math.pow(10, 8)
              _amount = Some(Coin.valueOf(bitcoin.toLong))
            }
            refreshAddress()
          }
        })
        handler.postDelayed(scheduled.get, 200)
      }
    })
  }

  def refreshAddress(): Unit = {
    qrCodeImageView.setImageBitmap(null)
    wallet.account(_accountIndex) flatMap {(account) =>
      account.freshPublicAddress()
    } flatMap {(address) =>
      addressTextView.setText(address.toString)
      val amount = _amount.map(_.toPlainString)
      val uri = new Builder()
        .scheme("bitcoin")
        .path(address.toString)
      if (amount.isDefined)
        uri.appendQueryParameter("amount", amount.get)
      QrCodeGenerator.from(getActivity, uri.build().toString.replace(":/", ":"), 250, 250)
    } onComplete {
        case Success(bitmap) => qrCodeImageView.setImageBitmap(bitmap)
        case Failure(ex) => ex.printStackTrace()
    }
  }

  def wallet = getActivity.asInstanceOf[WalletActivity].wallet

  override implicit def viewId2View[V <: View](id: Int): V = getView.findViewById(id)
    .asInstanceOf[V]

  private var _accountIndex = 0
  private var _amount: Option[Coin] = None

  override def isDestroyed: Boolean = false
}
