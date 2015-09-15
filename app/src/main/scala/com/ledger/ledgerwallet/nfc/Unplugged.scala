package com.ledger.ledgerwallet.nfc

import java.io.IOException

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.nfc.Tag
import com.ledger.ledgerwallet.bitlib.crypto.Bip39
import nordpol.IsoCard
import nordpol.android.{AndroidCard, OnDiscoveredTagListener}
import android.util.Log

class Unplugged extends OnDiscoveredTagListener {
  val TAG: String = "LedgerUnpluggedHelper"
  val APPLICATION_ID = "54BF6AA9"
  val SERVICE_ID = "test"
  val APPLICATION_APDU: String = "00a404000ca0000006170054bf6aa94901"
  val successfulAPDU = Array[Byte](0x90.toByte, 0x00);
  val FIDESMO_APP: String = "com.fidesmo.sec.android"
  val SERVICE_URI: String = "https://api.fidesmo.com/service/"
  val SERVICE_DELIVERY_CARD_ACTION: String = "com.fidesmo.sec.DELIVER_SERVICE"
  val SERVICE_DELIVERY_REQUEST_CODE: Int = 724
  val MARKET_URI: String = "market://details?id="
  val MARKET_VIA_BROWSER_URI: String = "http://play.google.com/store/apps/details?id="

  def tagDiscovered(tag: Tag) {
    try {
      //readCard(AndroidCard.get(tag))
      Log.v(TAG, "Found NFC Tag !")
    }
    catch {
      case ioe: IOException => {

      }
    }
  }

  def isLedgerUnplugged(tag: Tag): Boolean = {
    if(sendAPDU(AndroidCard.get(tag), Utils.decodeHex(APPLICATION_APDU)) == "9000"){ return true } else { return false }
  }

  def isFidesmoInstalled(activity: Activity): Boolean = {
    val pm: PackageManager = activity.getPackageManager
    var app_installed: Boolean = false
    try {
      pm.getPackageInfo(FIDESMO_APP, PackageManager.GET_ACTIVITIES)
      app_installed = true
    }
    catch {
      case e: PackageManager.NameNotFoundException => {
        app_installed = false
      }
    }

    app_installed
  }

  def installFidesmo(activity: Activity) {
//    activity.runOnUiThread(new Runnable() {
//      def run {
//        try {
//          activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI + FIDESMO_APP)))
//        }
//        catch {
//          return false
//        }
//      }
//    })
  }

  def launchCardletInstallation(activity: Activity) {
    if (isFidesmoInstalled(activity)) {
      try {
        val intent: Intent = new Intent(SERVICE_DELIVERY_CARD_ACTION, Uri.parse(SERVICE_URI + APPLICATION_ID + "/" + SERVICE_ID))
        activity.startActivityForResult(intent, SERVICE_DELIVERY_REQUEST_CODE)
      }
      catch {
        case e: IllegalArgumentException => {
          // Error parsing URI
        }
      }
    }
    else {
      installFidesmo(activity)
    }
  }

  def sendAPDU (card: IsoCard, APDU: Array[Byte]): Array[Byte] = {
    Log.v(TAG, "Sending APDU " + APDU)
    var response: Array[Byte] = null
    try {
      card.connect
      response = card.transceive(APDU)
      card.close
    }
    catch {
      case e: IOException => {
        Log.v(TAG, e.toString())
      }
    }

    response
  }

  def getBip32FromSeed(bip39: String): String = {
    Utils.bytesToHex(Bip39.generateSeedFromWordList(bip39.split(" "), "").getBip32Seed)
  }

  // Dirt cheap code for now
  def setup(card: IsoCard, PIN: String, seed: String): Array[Byte] ={ // This will be replaced by BTChip's Java code later
    val command = Array[Byte](0xe0.toByte, 0x20, 0x00, 0x00)
    val mode: Byte = 0x01
    val features: Byte = 0x0a
    val coinVersion: Byte = 0x00
    val p2shVersion: Byte = 0x05
    val pinHex: Array[Byte] = PIN.getBytes
    val pinLength: Byte = PIN.length.toByte
    val secPinLength: Byte = 0x00
    val bip32Seed: Array[Byte] = Utils.decodeHex(getBip32FromSeed(seed))
    val bip32SeedLength: Byte = bip32Seed.length.toByte
    val threedeskey: Byte = 0x00

    var APDU = Array[Byte]()
    APDU = APDU :+ mode :+ features :+ coinVersion :+ p2shVersion :+ pinLength
    APDU = APDU ++ pinHex :+ secPinLength :+ bip32SeedLength
    APDU = APDU ++ bip32Seed :+ threedeskey

    APDU = (command :+ APDU.length.toByte) ++ APDU

    sendAPDU(card, APDU)
  }
}
