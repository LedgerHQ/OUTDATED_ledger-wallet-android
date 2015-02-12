/**
 *
 * HomeActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 10/02/15.
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
package com.ledger.ledgerwallet.app

import android.content.Intent
import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.app.m2fa.{IncomingTransactionDialogFragment, PairedDonglesActivity}
import com.ledger.ledgerwallet.app.m2fa.pairing.CreateDonglePairingActivity
import com.ledger.ledgerwallet.base.{BigIconAlertDialog, BaseFragment, BaseActivity}
import com.ledger.ledgerwallet.bitcoin.AmountFormatter
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.api.m2fa.{GcmAPI, IncomingTransactionAPI}
import com.ledger.ledgerwallet.utils.logs.Logger
import com.ledger.ledgerwallet.utils.{GooglePlayServiceHelper, TR}
import com.ledger.ledgerwallet.widget.TextView
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._

import scala.util.{Failure, Success}

class HomeActivity extends BaseActivity {

  lazy val api = IncomingTransactionAPI.defaultInstance(context)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.single_fragment_holder_activity)
    ensureFragmentIsSetup()
  }

  override def onResume(): Unit = {
    super.onResume()
    ensureFragmentIsSetup()
    api onIncomingTransaction openIncomingTransactionDialog
    GooglePlayServiceHelper.getGcmRegistrationId onComplete {
      case Success(regId) => GcmAPI.defaultInstance.updateDonglesToken(regId)
      case Failure(ex) =>
    }
  }


  override def onPause(): Unit = {
    super.onPause()
    api.stop()
  }

  private[this] def openIncomingTransactionDialog(tx: IncomingTransactionAPI#IncomingTransaction): Unit = {
    new IncomingTransactionDialogFragment(tx).show(getSupportFragmentManager, IncomingTransactionDialogFragment.DefaultTag)
  }

  private[this] def ensureFragmentIsSetup(): Unit = {
    val dongleCount = PairedDongle.all.length
    if (dongleCount == 0 && getSupportFragmentManager.findFragmentByTag(HomeActivityContentFragment.NoPairedDeviceFragmentTag) == null) {
      getSupportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, new HomeActivityContentFragment, HomeActivityContentFragment.NoPairedDeviceFragmentTag)
        .commitAllowingStateLoss()
    } else if (dongleCount > 0 && getSupportFragmentManager.findFragmentByTag(HomeActivityContentFragment.PairedDeviceFragmentTag) == null) {
      getSupportFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, new HomeActivityContentFragment, HomeActivityContentFragment.PairedDeviceFragmentTag)
        .commitAllowingStateLoss()
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CreateDonglePairingActivity.CreateDonglePairingRequest) {
      resultCode match {
        case CreateDonglePairingActivity.ResultOk => showSuccessDialog(PairedDongle.all.sortBy(- _.createdAt.get.getTime).head.name.get)
        case CreateDonglePairingActivity.ResultNetworkError => showErrorDialog(R.string.pairing_failure_dialog_error_network)
        case CreateDonglePairingActivity.ResultPairingCancelled => showErrorDialog(R.string.pairing_failure_dialog_cancelled)
        case CreateDonglePairingActivity.ResultWrongChallenge => showErrorDialog(R.string.pairing_failure_dialog_wrong_answer)
        case _ =>
      }
    }
  }

  private[this] def showErrorDialog(contentTextId: Int): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_failure_dialog_title)
      .setContentText(contentTextId)
      .setIcon(R.drawable.ic_big_red_failure)
      .create().show(getSupportFragmentManager, "ErrorDialog")
  }

  private[this] def showSuccessDialog(dongleName: String): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_success_dialog_title)
      .setContentText(TR(R.string.pairing_success_dialog_content).as[String].format(dongleName))
      .setIcon(R.drawable.ic_big_red_failure)
      .create().show(getSupportFragmentManager, "SuccessDialog")
  }

}

object HomeActivityContentFragment {
  val PairedDeviceFragmentTag = "PairedDeviceFragmentTag"
  val NoPairedDeviceFragmentTag = "NoPairedDeviceFragmentTag"
}

class HomeActivityContentFragment extends BaseFragment {

  lazy val actionButton = TR(R.id.button).as[TextView]
  lazy val helpLink = TR(R.id.bottom_text).as[TextView]
  lazy val isInPairedDeviceMode = if (getTag == HomeActivityContentFragment.PairedDeviceFragmentTag) true else false
  lazy val isInNoPairedDeviceMode = !isInPairedDeviceMode

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    val layoutId = if (isInPairedDeviceMode) R.layout.home_activity_paired_device_fragment else R.layout.home_activity_no_paired_device_fragment
    inflater.inflate(layoutId, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    actionButton onClick {
      if (isInPairedDeviceMode) {
        val intent = new Intent(getActivity, classOf[PairedDonglesActivity])
        startActivity(intent)
      } else {
        val intent = new Intent(getActivity, classOf[CreateDonglePairingActivity])
        getActivity.startActivityForResult(intent, CreateDonglePairingActivity.CreateDonglePairingRequest)
      }
    }
    helpLink onClick {
      val intent = new Intent(Intent.ACTION_VIEW, Config.HelpCenterUri)
      startActivity(intent)
    }
  }
}