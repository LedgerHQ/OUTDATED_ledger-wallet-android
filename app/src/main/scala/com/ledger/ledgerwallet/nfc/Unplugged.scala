package com.ledger.ledgerwallet.nfc

import android.net.Uri
import android.nfc.Tag
import com.ledger.ledgerwallet.bitlib.crypto.Bip39
import scala.concurrent.ExecutionContext.Implicits.global
import com.ledger.ledgerwallet.utils.logs.Logger
import nordpol.android.AndroidCard
import scala.concurrent.Future

class Unplugged(val tag: Tag)  {

  type ByteArray = Array[Byte]
  type APDU = ByteArray

  implicit  val LoggerTag: String = "LedgerUnpluggedHelper"

  val APPLICATION_ID = "54BF6AA9"
  val SERVICE_ID = "test"
  val ApplicationApdu: String = "00a404000ca0000006170054bf6aa94901"
  val successfulAPDU = Array[Byte](0x90.toByte, 0x00);
  val FIDESMO_APP: String = "com.fidesmo.sec.android"
  val SERVICE_URI: String = "https://api.fidesmo.com/service/"
  val SERVICE_DELIVERY_CARD_ACTION: String = "com.fidesmo.sec.DELIVER_SERVICE"
  val SERVICE_DELIVERY_REQUEST_CODE: Int = 724
  val MARKET_URI: String = "market://details?id="
  val MARKET_VIA_BROWSER_URI: String = "http://play.google.com/store/apps/details?id="

  val card = AndroidCard.get(tag)

  def checkIsLedgerUnplugged(): Future[Boolean] = {
    send(ApplicationApdu)
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

  def setup(PIN: String, seed: String): Future[Unit] = { // This will be replaced by BTChip's Java code later
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
      if (Utils.encodeHex(result).contains("9000")) {
        throw new Exception(s"Invalid status - ${Utils.encodeHex(result) }")
      }
    }
  }

  def setKeycard(keycard: String): Future[Unit] = {
    val command = Array[Byte](0xd0.toByte, 0x26, 0x00, 0x00, 0x11, 0x04)
    val APDU = command ++ Utils.decodeHex(keycard)

    send(ApplicationApdu) flatMap { (result) =>
      send(APDU)
    } map { (result) =>
      if (!Utils.encodeHex(result).contains("9000")) {
        throw new Exception(s"Invalid status - ${Utils.encodeHex(result) }")
      }
    }
  }

}

object Unplugged {

  val FidesmoAppId = "54BF6AA9"
  val FidesmoAppPackageName = "com.fidesmo.sec.android"
  val FidesmoServiceUri = Uri.parse("https://api.fidesmo.com/service/")
  val FidesmoServiceRequestCode = 724
  val FidesmoServiceCardAction =  "com.fidesmo.sec.DELIVER_SERVICE"
  val FidesmoServiceId = "install"

}