/**
 *
 * DemoConnectedDeviceActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 21/01/16.
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

import android.app.ProgressDialog
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.text.InputType
import android.view.View
import android.widget.{TextView, Toast, Button}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{DeviceActivity, BaseActivity}
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.Device.Disconnect
import co.ledger.wallet.core.device.api.LedgerApi
import co.ledger.wallet.core.device.api.LedgerCommonApiInterface.LedgerApiInvalidAccessRightException
import co.ledger.wallet.core.utils.HexUtils
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.EditText
import co.ledger.wallet.wallet.DerivationPath
import org.bitcoinj.params.MainNetParams

import scala.util.{Try, Failure, Success}
import co.ledger.wallet.common._

class DemoConnectedDeviceActivity extends BaseActivity with DeviceActivity with ViewFinder {

  lazy val smallDataButton: Button = R.id.small_data_button
  lazy val bigDataButton: Button = R.id.big_data_button
  lazy val getVersionButton: Button = R.id.get_version_button
  lazy val getAttestationButton: Button = R.id.get_attestation_button
  lazy val getPublicAddressButton: Button = R.id.get_public_address
  lazy val getFirstAccountXpubButton: Button = R.id.get_first_account_xpub
  lazy val logView: TextView = R.id.log_view


  val SmallData = "Hello"
  val BigData = """Enter Hamlet.
                  |  Ham. To be, or not to be, that is the Question:
                  |Whether 'tis Nobler in the minde to suffer
                  |The Slings and Arrowes of outragious Fortune,
                  |Or to take Armes against a Sea of troubles,
                  |And by opposing end them: to dye, to sleepe
                  |No more;
                  |""".stripMargin

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.demo_connected_device_activity)
    smallDataButton onClick {
      exchangeData(SmallData)
    }
    bigDataButton onClick {
      exchangeData(BigData)
    }
    getVersionButton onClick {
      var d: Option[Device] = None
      smallDataButton.setEnabled(false)
      bigDataButton.setEnabled(false)
      connectedDevice flatMap {device =>
        d = Some(device)
        val api = LedgerApi(device)
        api.firmwareVersion()
      } onComplete {
        case Success(version) =>
          logView.append(s"GET VERSION => $version\n")
          smallDataButton.setEnabled(true)
          bigDataButton.setEnabled(true)
        case Failure(ex) =>
          ex.printStackTrace()
          Try(d.foreach(_.disconnect()))
          Toast.makeText(this, "Fail to send command", Toast.LENGTH_LONG).show()
          onDeviceDisconnection()
      }
    }
    getAttestationButton onClick {
      var d: Option[Device] = None
      smallDataButton.setEnabled(false)
      bigDataButton.setEnabled(false)
      connectedDevice flatMap {device =>
        d = Some(device)
        val api = LedgerApi(device)
        api.deviceAttestation()
      } onComplete {
        case Success(attestation) =>
          logView.append(s"--- GET DEVICE ATTESTATION ---\n")
          logView.append(s"attestation: $attestation\n")
          logView.append(s"------------------------------\n")
          smallDataButton.setEnabled(true)
          bigDataButton.setEnabled(true)
        case Failure(ex) =>
          ex.printStackTrace()
          Try(d.foreach(_.disconnect()))
          Toast.makeText(this, "Fail to send command", Toast.LENGTH_LONG).show()
          onDeviceDisconnection()
      }
    }
    getPublicAddressButton onClick getPublicAddress()
    getFirstAccountXpubButton onClick getXpub()
  }

  def getPublicAddress(attempts: Int = 0): Unit = {
    var d: Option[Device] = None
    var api: LedgerApi = null
    val progress = ProgressDialog.show(this, "Derivation", "Please wait")
    connectedDevice flatMap {device =>
      d = Some(device)
      api = LedgerApi(device)
      api.derivePublicAddress(DerivationPath("44'/0'/0'/0/0"), MainNetParams.get())
    } onComplete {
      case Success(result) =>
        progress.dismiss()
        logView.append(s"--- GET PUBLIC ADDRESS ---\n")
        logView.append(s"Address: ${result.address}\n")
        logView.append(s"------------------------------\n")
      case Failure(ex: LedgerApiInvalidAccessRightException) =>
        progress.dismiss()
        unlockDevice(api) {
          case Success(_) =>
            getPublicAddress(attempts + 1)
          case Failure(ex) =>
            ex.printStackTrace()
            Try(d.foreach(_.disconnect()))
            Toast.makeText(this, "Wrong pin code", Toast.LENGTH_LONG).show()
            onDeviceDisconnection()
        }
      case Failure(ex) =>
        progress.dismiss()
        ex.printStackTrace()
        Try(d.foreach(_.disconnect()))
        Toast.makeText(this, "Fail to send command", Toast.LENGTH_LONG).show()
        onDeviceDisconnection()
    }
  }

  def getXpub(attempts: Int = 0): Unit = {
    var d: Option[Device] = None
    var api: LedgerApi = null
    val progress = ProgressDialog.show(this, "Derivation", "Please wait")
    connectedDevice flatMap {device =>
      d = Some(device)
      api = LedgerApi(device)
      api.deriveExtendedPublicKey(DerivationPath("44'/0'/0'"), MainNetParams.get())
    } onComplete {
      case Success(result) =>
        progress.dismiss()
        logView.append(s"--- GET EXTENDED PUBLIC ADDRESS ---\n")
        logView.append(s"Address: ${result.serializePubB58(MainNetParams.get())}\n")
        logView.append(s"------------------------------\n")
      case Failure(ex: LedgerApiInvalidAccessRightException) =>
        progress.dismiss()
        unlockDevice(api) {
          case Success(_) =>
            getXpub(attempts + 1)
          case Failure(ex) =>
            ex.printStackTrace()
            Try(d.foreach(_.disconnect()))
            Toast.makeText(this, "Wrong pin code", Toast.LENGTH_LONG).show()
            onDeviceDisconnection()
        }
      case Failure(ex) =>
        progress.dismiss()
        ex.printStackTrace()
        Try(d.foreach(_.disconnect()))
        Toast.makeText(this, "Fail to send command", Toast.LENGTH_LONG).show()
        onDeviceDisconnection()
    }
  }

  private def unlockDevice(api: LedgerApi)(onComplete: (Try[Null]) => Unit) = {
    val input = new EditText(this)
    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
    new Builder(this)
      .setTitle("Unlock device")
      .setMessage("Enter PIN:")
      .setView(input)
      .setPositiveButton("Submit", new OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = {
          api.verifyPin(input.getText.toString).onComplete(onComplete)
        }
      })
      .setNegativeButton("Cancel", new OnClickListener {
        override def onClick(dialog: DialogInterface, which: Int): Unit = {

        }
      })
    .show()
  }

  override def onResume(): Unit = {
    super.onResume()
    connectedDevice onComplete {
      case Success(device) =>
        register(device.eventBus)
      case Failure(ex) =>
        ex.printStackTrace()
        onDeviceDisconnection()
    }
  }

  protected def exchangeData(data: String): Unit = {
    Logger.d("Just click on a button")
    smallDataButton.setEnabled(false)
    bigDataButton.setEnabled(false)
    var d: Option[Device] = None
    connectedDevice flatMap {(device) =>
      d = Some(device)
      val dataBytes = data.map(_.toByte).toArray
      val command =
        Array[Byte](0xE0.toByte, 0xFF.toByte, 0x00.toByte, 0x00, dataBytes.length.toByte) ++ dataBytes
      logView.append(s"=> ${HexUtils.bytesToHex(command)}\n")
      device.exchange(command)
    } map {(response) =>
      logView.append(s"<= ${HexUtils.bytesToHex(response)}\n")
    } onComplete {
      case Success(_) =>
        smallDataButton.setEnabled(true)
        bigDataButton.setEnabled(true)
      case Failure(ex) =>
        ex.printStackTrace()
        Try(d.foreach(_.disconnect()))
        Toast.makeText(this, "Fail to send command", Toast.LENGTH_LONG).show()
        onDeviceDisconnection()
    }
  }

  def onDeviceDisconnection(): Unit = {
    finish()
  }

  override def receive: Receive = {
    case Disconnect(_) => onDeviceDisconnection()
    case all =>
  }

  override implicit def viewId2View[V <: View](id: Int): V = findViewById(id).asInstanceOf[V]
}
