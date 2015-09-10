package com.ledger.ledgerwallet.nfc

import java.util.Arrays
import java.util.Locale
import scala.collection.JavaConversions._

object Utils {
  private val hexArray = "0123456789ABCDEF".toCharArray()
  private val AID_PREFIX = "A00000061700"
  private val AID_SUFFIX = "0101"
  private val SELECT_HEADER = "00A40400"

  def encodeHex(bytes: Array[Byte]): String = {
    val hexChars = Array.ofDim[Char](bytes.length * 2)
    for (i <- 0 until bytes.length) {
      val v = bytes(i) & 0xFF
      hexChars(i * 2) = hexArray(v >>> 4)
      hexChars(i * 2 + 1) = hexArray(v & 0x0F)
    }
    new String(hexChars)
  }

  def decodeHex(hexString: String): Array[Byte] = {
    if ((hexString.length & 0x01) != 0) {
      throw new IllegalArgumentException("Odd number of characters.")
    }
    val hexChars = hexString.toUpperCase(Locale.ROOT).toCharArray()
    val result = Array.ofDim[Byte](hexChars.length / 2)
    var i = 0
    while (i < hexChars.length) {
      result(i / 2) = (Arrays.binarySearch(hexArray, hexChars(i)) * 16 + Arrays.binarySearch(hexArray,
        hexChars(i + 1))).toByte
      i += 2
    }
    result
  }

  def bytesToHex(bytes: Array[Byte]): String = {
    val hexChars = Array.ofDim[Char](bytes.length * 2)
    for (j <- 0 until bytes.length) {
      val v = bytes(j) & 0xFF
      hexChars(j * 2) = hexArray(v >>> 4)
      hexChars(j * 2 + 1) = hexArray(v & 0x0F)
    }
    new String(hexChars)
  }

  def selectApdu(appId: String): Array[Byte] = {
    val cardletAid = AID_PREFIX + appId + AID_SUFFIX
    val aidLength = Array((cardletAid.length / 2).toByte)
    val selectApdu = SELECT_HEADER + Utils.encodeHex(aidLength) + cardletAid
    decodeHex(selectApdu)
  }

  def statusBytes(response: Array[Byte]): Array[Byte] = {
    Array(response(response.length - 2), response(response.length - 1))
  }

  def responseData(response: Array[Byte]): Array[Byte] = {
    Arrays.copyOfRange(response, 0, response.length - 2)
  }
}