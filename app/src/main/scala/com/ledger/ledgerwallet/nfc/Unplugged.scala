package com.ledger.ledgerwallet.nfc

import android.nfc.Tag
import com.ledger.ledgerwallet.bitlib.crypto.Bip39
import com.ledger.ledgerwallet.utils.logs.Logger
import nordpol.IsoCard
import nordpol.android.AndroidCard

import scala.concurrent.Future
import com.ledger.ledgerwallet.concurrent.ExecutionContext.Implicits.main

class Unplugged(val tag: Tag)  {

  type ByteArray = Array[Byte]
  type APDU = ByteArray

  implicit  val LoggerTag: String = "LedgerUnpluggedHelper"

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

  val card = AndroidCard.get(tag)

  def checkIsLedgerUnplugged(): Future[Boolean] = {
    send(APPLICATION_APDU)
      .map(Utils.encodeHex)
      .map(_ == "9000")
  }

  def send(apduString: String): Future[ByteArray] = send(Utils.decodeHex(apduString))
  def send(apduInt: Int*): Future[ByteArray] = send(apduInt.map(_.toByte).toArray)
  def send(apdu: APDU): Future[ByteArray] = Future {
    Logger.d(s"=> ${Utils.encodeHex(apdu)}")
    card.connect()
    val response = card.transceive(apdu)
    Logger.d(s"=> ${Utils.encodeHex(response)}")
    card.close()
    response
  }

  def checkIsSetup(): Future[Boolean] = {
    send(0xE0, 0x40, 0x00, 0x00, 0x0d, 0x03, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
    .map(_.slice(0, 4)).map(Utils.encodeHex).map(_ != "6985")
  }

  def setup(PIN: String, seed: String): Future[Boolean] = { // This will be replaced by BTChip's Java code later
    def getBip32FromSeed(bip39: String): String = {
      Utils.bytesToHex(Bip39.generateSeedFromWordList(bip39.split(" "), "").getBip32Seed)
    }

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

    send(APDU) map { (result) =>
      true
    }
  }

  def setKeycard(card: IsoCard, keycard: String): Future[Boolean] = {
    val command = Array[Byte](0xd0.toByte, 0x26, 0x00, 0x00, 0x11, 0x04)
    val APDU = command ++ Utils.decodeHex(keycard)

    send(APPLICATION_APDU) flatMap { (result) =>
      send(0xd0, 0x26, 0x00, 0x00, 0x11, 0x04)
    } map { (result) =>
      true
    }
  }


  /*
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
    activity.runOnUiThread(new Runnable() {
      def run {
        try {
          activity.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(MARKET_URI + FIDESMO_APP)))
        }
        catch {
          case e: Exception => {

          }
        }
      }
    })
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

  def selectApplication(card: IsoCard): Try[ByteArray] = {
    sendAPDU(card, Utils.decodeHex(APPLICATION_APDU))
  }

  def sendAPDU (card: IsoCard, APDU: Array[Byte]): Try[ByteArray] = Try {
    Log.v(TAG, "Sending APDU " + Utils.encodeHex(APDU))
    card.connect()
    val response = card.transceive(APDU)
    Log.v(TAG, "Response: " + Utils.encodeHex(response))
    card.close()
    response
  }



  def getBip32FromSeed(bip39: String): String = {
    Utils.bytesToHex(Bip39.generateSeedFromWordList(bip39.split(" "), "").getBip32Seed)
  }

  // Dirt cheap code for now
  def setup(card: IsoCard, PIN: String, seed: String): Try[ByteArray] = { // This will be replaced by BTChip's Java code later
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

  def setKeycard(card: IsoCard, keycard: String): Try[ByteArray] = {
    val command = Array[Byte](0xd0.toByte, 0x26, 0x00, 0x00, 0x11, 0x04)
    val APDU = command ++ Utils.decodeHex(keycard)

    selectApplication(card)
    sendAPDU(card, APDU)
  }
  */



}