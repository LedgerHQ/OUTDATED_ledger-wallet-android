/**
 *
 * UnlockDongleApi
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

import java.nio.charset.StandardCharsets

import co.ledger.wallet.dongle.exceptions.InvalidReponseStatusException
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

trait UnlockDongleApi extends DongleApi {

  def unlock(pinCode: String): Future[Unit] = {
    val APDU: Array[Byte] = (Array[Byte](0xE0.toByte, 0x22, 0x00, 0x00) :+ pinCode.length.toByte) ++ pinCode.getBytes(StandardCharsets.US_ASCII)
    send(APDU)(0x9000).recover {
      case InvalidReponseStatusException(status) =>
        if (((status & 0xFF00) >> 8) == 0x63) {
          throw new WrongPinCodeException(status - 0x63C0)
        } else {
          throw new WrongPinCodeException(0)
        }
      case exception => throw exception
    } map {(_) => }
  }

  def remainingAttemptsToUnlock(): Future[Int] = ???

}

case class WrongPinCodeException(remainingAttempts: Int) extends Exception(s"Wrong Pin code ($remainingAttempts)")