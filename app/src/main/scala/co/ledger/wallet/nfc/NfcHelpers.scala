package co.ledger.wallet.nfc

import android.app.Activity
import android.app.PendingIntent
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep

object NfcHelpers {

  def enableForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
    val intent = activity.getIntent
    intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP)
    if (adapter.isEnabled) {
      val tagIntent = PendingIntent.getActivity(activity, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT)
      val iso = new IntentFilter(NfcAdapter.ACTION_TECH_DISCOVERED)
      adapter.enableForegroundDispatch(activity, tagIntent, Array(iso), Array(Array(classOf[IsoDep].getName)))
    }
  }

  def disableForegroundDispatch(activity: Activity, adapter: NfcAdapter) {
    adapter.disableForegroundDispatch(activity)
  }

  def getIsoTag(intent: Intent): IsoDep = {
    val tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)
    if (tag != null) {
      IsoDep.get(tag)
    } else {
      null
    }
  }
}
