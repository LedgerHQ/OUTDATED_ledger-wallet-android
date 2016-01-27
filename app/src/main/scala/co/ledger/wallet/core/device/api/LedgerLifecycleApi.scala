/**
 *
 * LedgerLifecycleApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 26/01/16.
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
package co.ledger.wallet.core.device.api

import co.ledger.wallet.core.device.api.LedgerCommonApiInterface.LedgerApiException
import co.ledger.wallet.core.device.api.LedgerLifecycleApi.LedgerApiWrongPinCodeException
import co.ledger.wallet.core.utils.AsciiUtils

import scala.concurrent.Future
import scala.util.{Failure, Success}

trait LedgerLifecycleApi extends LedgerCommonApiInterface {
  import LedgerCommonApiInterface._

  def verifyPin(pin: String): Future[Null] = $("VERIFY PIN") {
    val bytes: Array[Byte] = AsciiUtils.toByteArray(pin)
    sendApdu(0xE0, 0x22, 0x00, 0x00, bytes, 0x00).map {(result) =>
      matchErrors(result) match {
        case Success(_) =>
          null
        case Failure(err: LedgerApiUnknownErrorException) =>
          if ((err.code & 0x6C00) == 0x6C00)
            throw new LedgerApiWrongPinCodeException(err.code - 0x6C00)
          else
            throw err
        case Failure(error) =>
          throw error
      }
    }
  }

  def remainingUnlockAttempts(): Future[Int] = $("VERIFY PIN 0x80") {
    val bytes: Array[Byte] = null
    sendApdu(0xE0, 0x22, 0x80, 0x00, bytes, 0x00).map {(result) =>
      matchErrors(result) match {
        case Success(_) =>
          3
        case Failure(err: LedgerApiUnknownErrorException) =>
          if ((err.code & 0x6C00) == 0x6C00)
            err.code - 0x6C00
          else
            throw err
        case Failure(error) =>
          throw error
      }
    }
  }

}

object LedgerLifecycleApi {

  case class LedgerApiWrongPinCodeException(remaining: Int) extends
    LedgerApiException(0x6C00 + remaining, s"Wrong pincode remaining $remaining")

}