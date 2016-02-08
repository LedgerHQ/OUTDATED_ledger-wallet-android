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
package co.ledger.wallet.app.ui

import java.security.KeyStore.PasswordProtection

import android.content.Intent
import android.os.Bundle
import android.view._
import android.widget.Toast
import co.ledger.wallet.R
import co.ledger.wallet.app.api.TeeAPI
import co.ledger.wallet.app.api.m2fa.{GcmAPI, IncomingTransactionAPI}
import co.ledger.wallet.app.ui.m2fa.pairing.CreateDonglePairingActivity
import co.ledger.wallet.app.ui.m2fa.{IncomingTransactionDialogFragment, PairedDonglesActivity}
import co.ledger.wallet.app.ui.unplugged.UnpluggedTapActivity
import co.ledger.wallet.app.base.{BaseActivity, BaseFragment, BigIconAlertDialog}
import co.ledger.wallet.common._
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main
import co.ledger.wallet.core.security.{Keystore, AndroidKeystore, ApplicationKeystore}
import co.ledger.wallet.core.utils.logs.{LogCatReader, Logger}
import co.ledger.wallet.core.utils.{AndroidUtils, GooglePlayServiceHelper, TR}
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.models.PairedDongle
import co.ledger.wallet.app.Config

import scala.util.{Failure, Success}


class HomeActivity extends BaseActivity {
  lazy val api = IncomingTransactionAPI.defaultInstance(context)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.single_fragment_holder_activity)
    ensureFragmentIsSetup()
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.home_activity_menu, menu)
    if (!AndroidUtils.hasNfcFeature()) {
      menu.findItem(R.id.setup_unplugged).setVisible(false)
    }
    menu.findItem(R.id.settings).setVisible(false)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)
    item.getItemId match {
      case R.id.export_logs =>
        exportLogs()
        true
      case R.id.settings =>
        startSettingsActivity()
        true
      case R.id.setup_unplugged =>
        startConfigureUnplugged()
        true
      case somethingElse => false
    }
  }

  def startConfigureUnplugged(): Unit =
    startActivity(new Intent(this, classOf[UnpluggedTapActivity]).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

  override def onResume(): Unit = {
    super.onResume()
    ensureFragmentIsSetup()
    api.listen()
    api onIncomingTransaction openIncomingTransactionDialog
    GooglePlayServiceHelper.getGcmRegistrationId onComplete {
      case Success(regId) =>
        GcmAPI.defaultInstance.updateDonglesToken(regId)
      case Failure(ex) =>
    }

    TrustletPromotionDialog.isShowable.onSuccess {
      case true =>
        TeeAPI.defaultInstance.isDeviceEligible.onSuccess {
          case true => runOnUiThread(TrustletPromotionDialog.show(getFragmentManager))
          case _ => // Nothing to do
        }
      case _ => // Nothing to do
    }

    refreshPairedDongleList()

    Keystore.defaultInstance match {
      case keystore: ApplicationKeystore =>
        if (!keystore.isInstalled) {
          startActivity(new Intent(this, classOf[InstallKeystoreActivity]))
        } else if (!keystore.isLoaded) {
          startActivity(new Intent(this, classOf[UnlockKeystoreActivity]))
        }
      case others => // Do nothing
    }

  }

  override def onPause(): Unit = {
    super.onPause()
    api.stop()
    api onIncomingTransaction null
  }

  private[this] def openIncomingTransactionDialog(tx: IncomingTransactionAPI#IncomingTransaction): Unit = {
    new IncomingTransactionDialogFragment(tx).show(getFragmentManager, IncomingTransactionDialogFragment.DefaultTag)
  }

  private[this] def ensureFragmentIsSetup(): Unit = {
    val dongleCount = PairedDongle.all.length
    if (dongleCount == 0 && getFragmentManager.findFragmentByTag(HomeActivityContentFragment.NoPairedDeviceFragmentTag) == null) {
      getFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, new HomeActivityContentFragment, HomeActivityContentFragment.NoPairedDeviceFragmentTag)
        .commitAllowingStateLoss()
    } else if (dongleCount > 0 && getFragmentManager.findFragmentByTag(HomeActivityContentFragment.PairedDeviceFragmentTag) == null) {
      getFragmentManager
        .beginTransaction()
        .replace(R.id.fragment_container, new HomeActivityContentFragment, HomeActivityContentFragment.PairedDeviceFragmentTag)
        .commitAllowingStateLoss()
    }
  }

  private[this] def refreshPairedDongleList(): Unit = {
   /*
    if (Keystore.defaultInstance) {
      // Display Unlock dialog
      return
    }
    var notifyDongleLost = false
    PairedDongle.all.foreach((dongle) => {
      if (dongle.pairingKey.isEmpty) {
        notifyDongleLost = true
        dongle.delete()
      }
    })

    if (notifyDongleLost) {
      // Display lost dongles warning dialog
    }
  */
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CreateDonglePairingActivity.CreateDonglePairingRequest) {
      resultCode match {
        case CreateDonglePairingActivity.ResultOk => showSuccessDialog(PairedDongle.all.sortBy(- _.createdAt.get.getTime).head.name.get)
        case CreateDonglePairingActivity.ResultNetworkError => showErrorDialog(R.string.pairing_failure_dialog_error_network)
        case CreateDonglePairingActivity.ResultPairingCancelled => showErrorDialog(R.string.pairing_failure_dialog_cancelled)
        case CreateDonglePairingActivity.ResultWrongChallenge => showErrorDialog(R.string.pairing_failure_dialog_wrong_answer)
        case CreateDonglePairingActivity.ResultTimeout => showErrorDialog(R.string.pairing_failure_dialog_timeout)
        case _ =>
      }
    }
  }

  private[this] def showErrorDialog(contentTextId: Int): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_failure_dialog_title)
      .setContentText(contentTextId)
      .setIcon(R.drawable.ic_big_red_failure)
      .create().show(getFragmentManager, "ErrorDialog")
  }

  private[this] def showSuccessDialog(dongleName: String): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_success_dialog_title)
      .setContentText(TR(R.string.pairing_success_dialog_content).as[String].format(dongleName))
      .setIcon(R.drawable.ic_big_green_success)
      .create().show(getFragmentManager, "SuccessDialog")
  }

  private[this] def exportLogs(): Unit = {
    LogCatReader.createEmailIntent(this).onComplete {
      case Success(intent) =>
        startActivity(intent)
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

  private[this] def startSettingsActivity(): Unit = startActivity(new Intent(this, classOf[SettingsActivity]))

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


    Option(view.findViewById(R.id.setup_unplugged)).foreach({ (view) =>
      if (!AndroidUtils.hasNfcFeature()) {
        view.setVisibility(View.GONE)
      }
      view.onClick(getActivity.asInstanceOf[HomeActivity].startConfigureUnplugged())
    })
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    //inflater.inflate(R.menu.home_activity_menu, menu)
  }
}