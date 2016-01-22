/**
 *
 * UsbDeviceImpl
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 22/01/16.
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
package co.ledger.wallet.core.device.usb

import android.content.Context
import android.hardware.usb.UsbDevice
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.DeviceManager.{ConnectivityTypes, ConnectivityType}
import co.ledger.wallet.core.utils.logs.Loggable
import de.greenrobot.event.EventBus

import scala.concurrent.Future

class UsbDeviceImpl(context: Context, override val name: String, usb: UsbDevice)
  extends Device
  with ThreadPoolTask
  with Loggable {

  override def connect(): Future[Device] = ???

  override def connectivityType: ConnectivityType = ConnectivityTypes.Usb

  override def debug_=(enable: Boolean): Unit = ???

  override def disconnect(): Unit = ???

  override def isDebugEnabled: Boolean = ???

  override def isExchanging: Boolean = ???

  override def isConnected: Boolean = ???

  override val eventBus: EventBus = new EventBus()

  @throws[AssertionError]("If there is already an exchange going on")
  override def exchange(command: Array[Byte]): Future[Array[Byte]] = ???

  override def isConnecting: Boolean = ???

  override def hashCode(): Int = name.hashCode

  override def equals(o: scala.Any): Boolean = o.isInstanceOf[UsbDeviceImpl] && o.hashCode() == hashCode()

}

