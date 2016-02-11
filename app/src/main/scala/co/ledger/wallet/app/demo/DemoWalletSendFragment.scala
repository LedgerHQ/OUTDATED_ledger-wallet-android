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

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.support.v7.widget.{AppCompatTextView, AppCompatEditText}
import android.text.{Editable, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.AdapterView.OnItemSelectedListener
import android.widget._
import co.ledger.wallet.core.bitcoin.BitcoinUtils
import co.ledger.wallet.wallet.{Utxo, Account}
import co.ledger.wallet.{common, R}
import co.ledger.wallet.core.base.{WalletActivity, BaseFragment}
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.{EditText, TextView}
import common._
import org.bitcoinj.core.Coin

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

class DemoWalletSendFragment extends BaseFragment with ViewFinder {

  val ScanQrCodeRequest = 0x21

  val Fees = Array(
    "Normal" -> Coin.parseCoin("0.00001"),
    "High" -> Coin.parseCoin("0.0001"),
    "Low" -> Coin.parseCoin("0.000001")
  )

  def accountSpinner: Spinner = R.id.accounts
  def feesSpinner: Spinner = R.id.fees
  def totalAmountTextView: AppCompatTextView = R.id.total_amount
  def amountEditText: AppCompatEditText = R.id.amount
  def addressEditText: AppCompatEditText = R.id.address
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

    sendButton onClick onSendClicked
    scanButton onClick onScanClicked
  }

  override def onResume(): Unit = {
    super.onResume()
  }

  def onSendClicked(): Unit = {
    val amount = Try(computeTotalAmount())
    val address = addressEditText.getText.toString
    if (amount.isFailure) {
      Toast.makeText(getActivity, "Invalid amount", Toast.LENGTH_SHORT).show()
    } else if (!BitcoinUtils.isAddressValid(address)) {
      Toast.makeText(getActivity, "Invalid address", Toast.LENGTH_SHORT).show()
    } else {
      Toast.makeText(getActivity, "Send it now", Toast.LENGTH_SHORT).show()
    }
  }

  def onScanClicked(): Unit = {
    startActivityForResult(new Intent(getActivity, classOf[DemoQrCodeScannerActivity]), ScanQrCodeRequest)
  }

  def computeTotalAmount(): Coin = {
    val amount = Coin.parseCoin(amountEditText.getText.toString)
    val fees = Fees(feesSpinner.getSelectedItemPosition)._2
    val total = amount add fees
    totalAmountTextView.setText(s"Total: ${total.toFriendlyString}")
    total
  }

  def refreshUtxoList(): Unit = {
    val selectedAccount = accountSpinner.getSelectedItemPosition
    if (_currentAccount != selectedAccount) {
      _currentAccount = selectedAccount
      wallet.account(selectedAccount).flatMap(_.utxo()) onComplete {
        case Success(utxo) =>
          _utxo = Some(utxo)
        case Failure(ex) =>
          ex.printStackTrace()
      }
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == ScanQrCodeRequest && resultCode == Activity.RESULT_OK) {
      import DemoQrCodeScannerActivity._
      addressEditText.setText(data.getStringExtra(Address))
      if (data.hasExtra(Amount))
        amountEditText.setText(data.getStringExtra(Amount))
    }
  }

  private def setupUi(): Unit = {
    wallet.accounts() flatMap {(accounts) =>
      Future.sequence(accounts.map(_.balance()).toList) map {
        accounts -> _.toArray
      }
    } foreach {
      case (accounts: Array[Account], balances: Array[Coin]) =>
        val display = accounts.indices map {(index) =>
          s"Account #$index (${balances(index).toFriendlyString})"
        }
        val adapter = new ArrayAdapter[String](getActivity, android.R.layout.simple_list_item_1,
          display.toArray)
        accountSpinner.setAdapter(adapter)
        accountSpinner.setOnItemSelectedListener(new OnItemSelectedListener {
          override def onNothingSelected(parent: AdapterView[_]): Unit = ()

          override def onItemSelected(parent: AdapterView[_], view: View, position: Int, id:
          Long): Unit = {
            refreshUtxoList()
          }
        })
        refreshUtxoList()
    }
    val fees = Fees.map({case (name, _) => name})
    val adapter = new ArrayAdapter[String](getActivity, android.R.layout.simple_list_item_1, fees)
    feesSpinner.setAdapter(adapter)
    feesSpinner.setOnItemSelectedListener(new OnItemSelectedListener {
      override def onNothingSelected(parent: AdapterView[_]): Unit = ()

      override def onItemSelected(parent: AdapterView[_], view: View, position: Int, id: Long):
      Unit = {
        Try(computeTotalAmount())
      }
    })
    amountEditText.removeTextChangedListener(_textWatcher)
    amountEditText.addTextChangedListener(_textWatcher)
  }

  def wallet = getActivity.asInstanceOf[WalletActivity].wallet

  override implicit def viewId2View[V <: View](id: Int): V = getView.findViewById(id)
    .asInstanceOf[V]

  private val _textWatcher = new TextWatcher {
    override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = ()

    override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = ()

    override def afterTextChanged(s: Editable): Unit = Try(computeTotalAmount())
  }

  private var _utxo: Option[Array[Utxo]] = None
  private var _currentAccount = -1
}
