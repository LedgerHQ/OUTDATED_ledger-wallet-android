package com.ledger.ledgerwallet.nfc

import java.io.IOException
import java.util.Arrays

import android.nfc.Tag
import com.ledger.ledgerwallet.bitlib.crypto.Bip39
import nordpol.IsoCard
import nordpol.android.{AndroidCard, OnDiscoveredTagListener}
import android.util.Log
class Unplugged extends OnDiscoveredTagListener {
  val TAG: String = "LedgerUnpluggedHelper"
  val APPLICATION_APDU: String = "00a404000ca0000006170054bf6aa94901"
  val successfulAPDU = Array[Byte](0x90.toByte, 0x00);

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
    if(sendAPDU(AndroidCard.get(tag), APPLICATION_APDU) == "9000"){ return true } else { return false }
  }

  def sendAPDU (card: IsoCard, APDU: String): String = {
    Log.v(TAG, "Sending APDU " + APDU)
    var response: Array[Byte] = null
    try {
      card.connect
      response = card.transceive(Utils.decodeHex(APDU))
      card.close
    }
    catch {
      case e: IOException => {
        // Add error handling
        Log.v(TAG, e.toString())
      }
    }

    return Utils.encodeHex(response)
  }

  def getBip32FromSeed(bip39: String): String = {
    Utils.bytesToHex(Bip39.generateSeedFromWordList(bip39.split(" "), "").getBip32Seed)
  }

  // Dirt cheap code for now
  def setup(PIN: String, seed: String): Unit ={ // This will be replaced by BTChip's Java code later
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
    Log.v("LedgerBIP39", Utils.encodeHex(APDU))
  }
}
