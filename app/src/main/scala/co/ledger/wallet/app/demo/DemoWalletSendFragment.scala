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

import java.util.UUID

import android.app.{ProgressDialog, Activity}
import android.content.{DialogInterface, Intent}
import android.net.Uri
import android.os.{Handler, Bundle}
import android.support.v7.app.AlertDialog.Builder
import android.support.v7.widget.{AppCompatEditText, AppCompatTextView}
import android.text.{Editable, InputType, TextWatcher}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.AdapterView.OnItemSelectedListener
import android.widget._
import co.ledger.wallet.R
import co.ledger.wallet.common._
import co.ledger.wallet.core.base.{BaseFragment, DeviceActivity, WalletActivity}
import co.ledger.wallet.core.bitcoin.BitcoinUtils
import co.ledger.wallet.core.bitcoin.BitcoinUtils.DefaultUtxoPickPolicy
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.api.LedgerTransactionApi.{KeyCardSignatureValidationRequest, SignatureNeeds2FAValidationException, SignatureValidationRequest}
import co.ledger.wallet.core.device.api.{LedgerApi, LedgerTransactionEasyApi}
import co.ledger.wallet.core.net.HttpClient
import co.ledger.wallet.core.utils.BytesWriter
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.EditText
import co.ledger.wallet.wallet.{Account, Utxo}
import org.bitcoinj.core.{Address => JAddress, Coin}
import org.json.JSONObject

import scala.collection.mutable
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class DemoWalletSendFragment extends BaseFragment with ViewFinder {

  val ScanQrCodeRequest = 0x21
  val ConnectDeviceRequest = 0x42

  var Fees: Future[Array[(String, Coin)]] = fetchRecommendedFees()

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
    _txRequest foreach {(request) =>
      request.resumeUi()
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    _txRequest foreach {(request) =>
      request.pauseUi()
    }
  }

  def onSendClicked(): Unit = {
    if (_utxo.isEmpty) {
      Toast.makeText(this, "Computing fees please wait", Toast.LENGTH_SHORT).show()
    } else {
      val amount = Try(computeTotalAmount())
      val address = addressEditText.getText.toString
      if (amount.isFailure) {
        Toast.makeText(getActivity, "Invalid amount", Toast.LENGTH_SHORT).show()
      } else if (!BitcoinUtils.isAddressValid(address)) {
        Toast.makeText(getActivity, "Invalid address", Toast.LENGTH_SHORT).show()
      } else {
        val value = Coin.parseCoin(amountEditText.getText.toString)
        val fees = amount.get.subtract(value)
        val accountIndex = accountSpinner.getSelectedItemPosition
        _txRequest = Some(new CreateTransactionRequest(
          pickUtxo(amount.get).get, new JAddress(null, address), value, fees, accountIndex))
        _txRequest.get.perform()
      }
    }
  }

  def connectDevice(): Unit = {
    val intent = new Intent(this, classOf[DemoDiscoverDeviceActivity])
    intent.putExtra(DemoDiscoverDeviceActivity.ExtraRequestType, DemoDiscoverDeviceActivity.ReconnectRequest)
    startActivityForResult(intent, ConnectDeviceRequest)
  }

  def onScanClicked(): Unit = {
    startActivityForResult(new Intent(getActivity, classOf[DemoQrCodeScannerActivity]), ScanQrCodeRequest)
  }

  def pickUtxo(amount: Coin): Option[Array[Utxo]] = {
    val picker = new DefaultUtxoPickPolicy(1)
    picker.pick(_utxo.get, amount)
  }

  def computeTotalAmount(): Coin = {
    import BitcoinUtils._

    if (Fees.isCompleted) {
      val amount = Coin.parseCoin(amountEditText.getText.toString)
      val feesPerB = Fees.value.get.get(feesSpinner.getSelectedItemPosition)._2.divide(1000)
      val fees: Coin = _utxo match {
        case Some(inputs) =>
          def computeUtxoAndFees(amount: Coin): Coin = {
            val utxo = pickUtxo(amount)
            if (utxo.isDefined) {
              val s = estimateTransactionSize(utxo.get, 2) // TODO: Do not consider that we always need change
              val v = feesPerB multiply s.max
              val utxoAmount = Coin.valueOf(utxo.get.map(_.value.getValue).sum)
              if (utxoAmount.isLessThan(amount add v)) {
                computeUtxoAndFees(amount add v)
              } else {
                totalAmountTextView.setText(s"Total: ${(amount add v).toFriendlyString} (estimated " +
                  s"size ${s.min} - ${s.max} bytes)")
                v
              }
            } else {
              totalAmountTextView.setText(s"You don't have enough funds")
              throw new Exception()
            }
          }
          computeUtxoAndFees(amount)
        case None =>
          totalAmountTextView.setText(s"Computing total amount, please wait...")
          feesPerB multiply 1000
      }
      amount add fees
    } else {
      totalAmountTextView.setText(s"Fetching transaction fees please wait...")
      Coin.ZERO
    }
  }

  def refreshUtxoList(): Unit = {
    val selectedAccount = accountSpinner.getSelectedItemPosition
    if (_currentAccount != selectedAccount) {
      _currentAccount = selectedAccount
      _utxo = None
      wallet.account(selectedAccount).flatMap(_.utxo()) onComplete {
        case Success(utxo) =>
          _utxo = Some(utxo)
          computeTotalAmount()
        case Failure(ex) =>
          ex.printStackTrace()
      }
    }
  }

  def fetchRecommendedFees(): Future[Array[(String, Coin)]] = {
    val client = new HttpClient(Uri.parse("http://bitcoinfees.21.co"))
    client.get("/api/v1/fees/recommended").json recoverWith {
      case all => // Retry
        all.printStackTrace()
        val promise = Promise[Array[(String, Coin)]]()
        new Handler().postDelayed(new Runnable {
          override def run(): Unit = promise.completeWith(fetchRecommendedFees())
        }, 2 * 60 * 1000)
        promise.future
    } map {
      case (json: JSONObject, r) =>
        val fees = new Array[(String, Coin)](3)
        fees(0) = "High (10 minutes)" -> Coin.valueOf(json.getLong("fastestFee")).multiply(1000)
        fees(1) = "Normal (30 minutes)" -> Coin.valueOf(json.getLong("halfHourFee")).multiply(1000)
        fees(2) = "Low (1 hour)" -> Coin.valueOf(json.getLong("hourFee")).multiply(1000)
        fees
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == ScanQrCodeRequest && resultCode == Activity.RESULT_OK) {
      import DemoQrCodeScannerActivity._
      addressEditText.setText(data.getStringExtra(Address))
      if (data.hasExtra(Amount))
        amountEditText.setText(data.getStringExtra(Amount))
    } else if (requestCode == ConnectDeviceRequest) {
      if (resultCode == Activity.RESULT_OK) {
        val deviceUuid = data.getStringExtra(DemoDiscoverDeviceActivity.ExtraDeviceUuid)
        getActivity.asInstanceOf[DeviceActivity].connectedDeviceUuid = UUID.fromString(deviceUuid)
        val f = getActivity.asInstanceOf[DeviceActivity].connectedDevice
        _txRequest.foreach(_.devicePromise.completeWith(f))
      } else {
        _txRequest.foreach(_.devicePromise.failure(new Exception("Unable to reconnect device")))
      }
    }
  }

  private def resetUi(): Unit = {
    addressEditText.setText("")
    amountEditText.setText("")
    totalAmountTextView.setText("")
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
    Fees foreach {(f) =>
      val fees = f.map({case (name, _) => name})
      val adapter = new ArrayAdapter[String](getActivity, android.R.layout.simple_list_item_1, fees)
      feesSpinner.setAdapter(adapter)
      feesSpinner.setOnItemSelectedListener(new OnItemSelectedListener {
        override def onNothingSelected(parent: AdapterView[_]): Unit = ()

        override def onItemSelected(parent: AdapterView[_], view: View, position: Int, id: Long):
        Unit = {
          Try(computeTotalAmount())
        }
      })
      computeTotalAmount()
    }
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
  private var _txRequest: Option[CreateTransactionRequest] = None

  class CreateTransactionRequest(utxo: Array[Utxo],
                                 address: JAddress,
                                 value: Coin,
                                 fees: Coin,
                                 accountIndex: Int) {


    def perform(): Unit = {
      Toast.makeText(getActivity, "Performing...", Toast.LENGTH_LONG).show()
      val progressDialog = new ProgressDialog(getActivity)
      progressDialog.setTitle("Transaction creation")
      progressDialog.setMessage("Please wait...")
      progressDialog.setIndeterminate(false)
      progressDialog.setProgressNumberFormat("")
      progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL)
      progressDialog.setMax(100)
      progressDialog.setProgress(0)
      progressDialog.show()
      val deviceActivity = getActivity.asInstanceOf[DeviceActivity]
      var transaction: LedgerTransactionEasyApi#TransactionBuilder = null
      def onProgress: (Int, Int) => Unit = {(current, total) =>
        val percent = ((current.toFloat / total.toFloat) * 100f).toInt
        progressDialog.setProgress(percent)
      }
      progressDialog.show()
      // Connect device
      deviceActivity.connectedDevice recoverWith {
        case all =>
          connectDevice()
          devicePromise.future
      } map(LedgerApi(_)) flatMap {(api) =>
        transaction = api.buildTransaction()
        transaction
          .from(utxo)
          .to(address, value)
          .fees(fees)
        transaction.onProgress(onProgress)
        wallet.account(accountIndex).flatMap(_.freshChangeAddress())
      } flatMap {
        case (address, path) =>
          transaction.change(path, address)
          transaction.sign()
      } recoverWith {
        // Time for 2FA
        case ex: SignatureNeeds2FAValidationException =>
          handle2FaRequest(ex.validation).flatMap {(response) =>
            transaction.complete2FA(response)
            transaction.sign()
          }
        case ex: UnsupportedOperationException =>
          transaction.sign()
        case ex: Throwable =>
          throw ex
      } flatMap {(tx) =>
        Logger.d(s"Tx : $tx")("TX")
        Future.successful()
        //wallet.pushTransaction(tx)
      } onComplete {
          case Success(tx) =>
            progressDialog.dismiss()
            _utxo = None
            _currentAccount = -1
            refreshUtxoList()
            Toast.makeText(getActivity, s"Transaction succeed",
              Toast
                .LENGTH_LONG).show()
            _txRequest = None
            resetUi()
          case Failure(ex) =>
            progressDialog.dismiss()
            _currentAccount = -1
            _utxo = None
            refreshUtxoList()
            ex.printStackTrace()
            Toast.makeText(getActivity, s"Failed to sign transaction: ${ex.getMessage}", Toast
              .LENGTH_LONG).show()
            _txRequest = None
      }
    }

    def handle2FaRequest(request: SignatureValidationRequest): Future[Array[Byte]] = {
      val promise = Promise[Array[Byte]]()
      request match {
        case r: KeyCardSignatureValidationRequest =>
          val input = new EditText(getActivity)
          input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD)
          new Builder(getActivity)
            .setTitle("Resolve 2FA")
            .setMessage(s"Enter PIN for ${r.characters(addressEditText.getText.toString)}:")
            .setView(input)
            .setPositiveButton("Submit", new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
                val r = Integer.parseInt(input.getText.toString.split("").map("0" + _).mkString(""), 16)
                val writer = new BytesWriter()
                writer.writeInt(r)
                promise.success(writer.toByteArray)
              }
            })
            .setNegativeButton("Cancel", new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
                promise.failure(new Exception("Cancel"))
              }
            })
            .show()
        case others => promise.failure(new Exception("Not implemented 2FA validation"))
      }
      promise.future
    }

    def resumeUi(): Unit = {
      _isPaused = false
      dequeueUiUpdate()
    }

    def pauseUi(): Unit = {
      _isPaused = true
    }

    private def updateUi(handler: () => Unit): Unit = {
      _uiUpdates.enqueue(handler)
      dequeueUiUpdate()
    }

    private def dequeueUiUpdate(): Unit = {
      if (!_isPaused && _uiUpdates.nonEmpty) {
        val handler = _uiUpdates.dequeue()
        handler()
      }
    }

    private val _uiUpdates = new mutable.Queue[() => Unit]()
    private var _isPaused = false
    val devicePromise = Promise[Device]()
  }

}

