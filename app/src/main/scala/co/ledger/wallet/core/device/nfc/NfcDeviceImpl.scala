/**
 *
 * NfcDeviceImpl
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 05/02/16.
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
package co.ledger.wallet.core.device.nfc

import android.nfc.tech.IsoDep
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.DeviceManager.{ConnectivityType, ConnectivityTypes}
import de.greenrobot.event.EventBus

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

class NfcDeviceImpl(tag: IsoDep, executionContext: ExecutionContext) extends Device {

  implicit val ec = executionContext

  override def connect(): Future[Device] = Future.successful(this)

  override def connectivityType: ConnectivityType = ConnectivityTypes.Nfc

  override def debug_=(enable: Boolean): Unit = ()

  override def disconnect(): Unit = Future {
    tag.close()
  }

  override def isDebugEnabled: Boolean = false

  override def readyForExchange: Future[Unit] = _exchanger.map(_.future).getOrElse(Future.successful())

  override def isExchanging: Boolean = _exchanger.isDefined

  override def name: String = "NFC tag"

  override def isConnected: Boolean = true

  override val eventBus: EventBus = new EventBus()

  @throws[AssertionError]("If there is already an exchange going on")
  override def exchange(command: Array[Byte]): Future[Array[Byte]] = synchronized {
    assert(_exchanger.isEmpty, "There is already an exchange going on")
    _exchanger = Some(new Exchanger)
    _exchanger.get.exchange(command)
  }

  override def hashCode(): Int = tag.getTag.hashCode

  override def equals(o: scala.Any): Boolean = o.isInstanceOf[NfcDeviceImpl] && o.hashCode() == hashCode()

  override def isConnecting: Boolean = false

  private var _exchanger: Option[Exchanger] = None

  private class Exchanger {

    def future = _promise.future

    def exchange(command: Array[Byte]): Future[Array[Byte]] = Future {
      if (!tag.isConnected) {
        tag.connect()
        tag.setTimeout(30000)
      }
      tag.transceive(command)
    } recover {
      case throwable: Throwable =>
        Try(tag.close())
        throw throwable
    } andThen {
      case Success(_) =>
        NfcDeviceImpl.this.synchronized {
          _exchanger = None
        }
        _promise.success()
      case Failure(ex) =>
        _promise.failure(ex)
    }

    private val _promise = Promise[Unit]()

  }

  override def info: String = ""
}
