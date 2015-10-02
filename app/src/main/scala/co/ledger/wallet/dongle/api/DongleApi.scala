/**
 *
 * DongleApi
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

import co.ledger.wallet.nfc.Utils
import com.btchip.comm.BTChipTransport

import scala.concurrent.Future

trait DongleApi {

  type ByteArray = Array[Byte]
  type APDU = ByteArray

  def transport: BTChipTransport
  def send(apduString: String)(acceptedStatuses: Int*)
    : Future[ByteArray] = send(Utils.decodeHex(apduString))(acceptedStatuses: _*)

  def send(apduInt: Int*)(acceptedStatuses: Int*)
    : Future[ByteArray] = send(apduInt.map(_.toByte).toArray)(acceptedStatuses: _*)

  def send(apdu: APDU)(acceptedStatuses: Int*): Future[ByteArray]

  protected[this] def lastStatusWord_=(statusWord: Int): Unit
  def lastStatusWord: Int
}
