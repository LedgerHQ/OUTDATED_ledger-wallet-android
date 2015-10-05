package co.ledger.wallet.nfc

import android.net.Uri

object Unplugged {

  val FidesmoAppId = "54BF6AA9"
  val FidesmoAppPackageName = "com.fidesmo.sec.android"
  val FidesmoServiceUri = Uri.parse("https://api.fidesmo.com/service/")
  val FidesmoServiceRequestCode = 724
  val FidesmoServiceCardAction =  "com.fidesmo.sec.DELIVER_SERVICE"
  val FidesmoInstallServiceId = "install"
  val FidesmoDeleteServiceId = "delete"
}