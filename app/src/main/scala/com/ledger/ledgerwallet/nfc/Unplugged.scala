package com.ledger.ledgerwallet.nfc

import java.io.IOException
import java.util.Arrays

import android.nfc.Tag
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
    /*val response = sendAPDU(AndroidCard.get(tag), APPLICATION_APDU)
    if (response != null && Arrays.equals(Utils.statusBytes(response), successfulSelectApdu)) {
      return true
    }else{
      return false
    }*/

    if(sendAPDU(AndroidCard.get(tag), APPLICATION_APDU) == "9000"){ return true } else { return false }
  }

  def sendAPDU (card: IsoCard, APDU: String): String = {
    Log.v(TAG, "Sending APDU " + APDU)
    var response: Array[Byte] = null
    try {
      Log.v(TAG, "Card connect")
      card.connect
      Log.v(TAG, "Card transceive")
      response = card.transceive(Utils.decodeHex(APDU))
      Log.v(TAG, "Card close")
      card.close
      Log.v(TAG, "Response: " + Utils.encodeHex(response))
    }
    catch {
      case e: IOException => {
        // Add error handling
        Log.v(TAG, e.toString())
      }
    }

    return Utils.encodeHex(response)
  }
}
