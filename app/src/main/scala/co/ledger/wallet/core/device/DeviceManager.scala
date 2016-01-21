/**
 *
 * DeviceManager
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 15/01/16.
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
package co.ledger.wallet.core.device

import java.util.UUID

import android.content.Context
import co.ledger.wallet.core.device.ble.BleDeviceManager

import scala.concurrent.{ExecutionContext, Future}

trait DeviceManager {
  import DeviceManager._

  implicit val ec: ExecutionContext

  def compatibleConnectivityTypes: Future[Set[ConnectivityType]] = Future {
    _deviceManager filter {
      case (t, m) => m.isCompatible
    } map {
      case (t, m) => t
    } toSet
  }

  def deviceManager(connectivityType: ConnectivityType): DeviceConnectionManager = {
    _deviceManager(connectivityType)
  }

  def registerDevice(device: Device): Future[UUID] = Future {
    val uuid = UUID.randomUUID()
    _registeredDevices(uuid) = device
    uuid
  }

  def unregisterDevice(uuid: UUID): Unit = Future {
    _registeredDevices.remove(uuid)
  }

  def unregisterDevice(device: Device): Unit = Future {
    // TODO: Rewrite
    _registeredDevices.retain((uuid, d) => d != device)
  }

  def connectedDevice(uuid: UUID): Future[Device] = Future {
    _registeredDevices.getOrElse(uuid, throw new Exception("No such device"))
  }

  def attemptReconnectLastDevice(): Future[Device] = {
    null
  }

  def context: Context

  protected[this] val _registeredDevices = scala.collection.mutable.Map[UUID, Device]()

  import DeviceManager.ConnectivityTypes._
  private[this] lazy val _deviceManager = Map[ConnectivityType, DeviceConnectionManager](
    Ble -> new BleDeviceManager(context, ec)
  )

}

object DeviceManager {

  object ConnectivityTypes extends Enumeration {
    type ConnectivityType = Value
    val Usb, Ble, Tee, Nfc = Value
  }
  type ConnectivityType = ConnectivityTypes.ConnectivityType

  case class AndroidDeviceNotCompatibleException(msg: String) extends Exception(msg)
  case class MissingPermissionException(msg: String) extends Exception(msg)
  case class DisabledServiceException(msg: String) extends Exception(msg)
}


