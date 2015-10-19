/**
 *
 * SetupDongleApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 02/10/15.
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
package co.ledger.wallet.dongle.api

import co.ledger.wallet.bitcoin.Bip39Helper
import co.ledger.wallet.utils.HexUtils
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait SetupDongleApi extends DongleApi {

  def setup(PIN: String, seed: String): Future[Unit] = {
    def getBip32FromSeed(bip39: String): String = {
      HexUtils.bytesToHex(Bip39Helper.getSeedFromMnemonicPhrase(bip39))
    }

    val command = Array[Byte](0xe0.toByte, 0x20, 0x00, 0x00)
    val mode: Byte = 0x01
    val features: Byte = 0x0a
    val coinVersion: Byte = 0x00
    val p2shVersion: Byte = 0x05
    val pinHex: Array[Byte] = PIN.getBytes
    val pinLength: Byte = PIN.length.toByte
    val secPinLength: Byte = 0x00
    val bip32Seed: Array[Byte] = HexUtils.decodeHex(getBip32FromSeed(seed))
    val bip32SeedLength: Byte = bip32Seed.length.toByte
    val threedeskey: Byte = 0x00

    var APDU = Array[Byte]()
    APDU = APDU :+ mode :+ features :+ coinVersion :+ p2shVersion :+ pinLength
    APDU = APDU ++ pinHex :+ secPinLength :+ bip32SeedLength
    APDU = APDU ++ bip32Seed :+ threedeskey

    APDU = (command :+ APDU.length.toByte) ++ APDU
    send(APDU)(0x9000).map((_) => {})
  }

  def setKeycard(keycard: String): Future[Unit] = {
    val command = Array[Byte](0xd0.toByte, 0x26, 0x00, 0x00, 0x11, 0x04)
    val APDU = command ++ HexUtils.decodeHex(keycard)
    send(APDU)(0x9000).map((_) => {})
  }


  def checkIsSetup(): Future[Boolean] = {
    send(0xE0, 0x40, 0x00, 0x00, 0x0d, 0x03, 0x80, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)()
      .map { (result) =>
      lastStatusWord != 0x6985
    }
  }
}
