/**
 *
 * NfcTransport
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
package co.ledger.wallet.dongle.transport

import android.nfc.Tag
import android.nfc.tech.IsoDep
import co.ledger.wallet.utils.logs.Logger
import com.btchip.BTChipException
import com.btchip.comm.BTChipTransport
import com.btchip.utils.Dump
import nordpol.android.AndroidCard

import scala.util.Try

class NfcTransport(val tag: Tag, val timeout: Int = NfcTransport.DefaultTimeout) extends BTChipTransport {

  override def close(): Unit = ???

  override def exchange(bytes: Array[Byte]): Array[Byte] = {
    implicit val LogTag = "NfcTransport"
    implicit val DisableLogging = !_debug

    val response = Try {
      if (!_card.isConnected) {
        _card.connect()
        _card.setTimeout(timeout)
        Logger.d("Connected")
      }

      Logger.d(s"=> ${Dump.dump(bytes)}")
      val response = _card.transceive(bytes)
      Logger.d(s"<= ${Dump.dump(response)}")
      response
    }
    response.getOrElse {
      Try(close())
      throw new BTChipException("I/O error", response.failed.get)
    }
  }

  override def setDebug(b: Boolean): Unit = _debug = b

  private[this] var _debug = false
  private[this] val _card = AndroidCard.get(tag)
}

object NfcTransport {

  val DefaultTimeout = 30000

}