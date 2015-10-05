/**
 *
 * Dongle
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 01/10/15.
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
package co.ledger.wallet.dongle

import co.ledger.wallet.dongle.api.{DongleApi, SetupDongleApi, UnlockDongleApi}
import co.ledger.wallet.dongle.exceptions.{CommuncationErrorException, InvalidReponseStatusException}
import com.btchip.comm.BTChipTransport

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class Dongle(val transport: BTChipTransport) extends DongleApi
  with SetupDongleApi
  with UnlockDongleApi {

  transport.setDebug(true)

  override def send(apdu: APDU)(acceptedStatuses: Int*): Future[ByteArray] = Future {
    val response = transport.exchange(apdu)
    if (response.length < 2) {
      throw new CommuncationErrorException("Truncated response")
    } else {
      lastStatusWord = (response(response.length - 2) & 255) << 8 | response(response.length - 1) & 255
      val result = new ByteArray(response.length - 2)
      System.arraycopy(response, 0, result, 0, response.length - 2)
      result
    }
  } map { (result) =>
    if (acceptedStatuses.isEmpty || (acceptedStatuses.nonEmpty &&
        acceptedStatuses.contains(lastStatusWord))) {
      result
    } else {
      throw new InvalidReponseStatusException(lastStatusWord)
    }
  }

  private[this] var _lastStatusWord = 0
  override protected[this] def lastStatusWord_=(statusWord: Int): Unit = _lastStatusWord = statusWord
  override def lastStatusWord: Int = _lastStatusWord
}
