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

import android.app.Activity
import android.content.Context
import co.ledger.wallet.core.device.DeviceFactory.{DeviceLost, DeviceDiscovered, ScanRequest}
import co.ledger.wallet.core.device.ble.BleDeviceFactory
import co.ledger.wallet.core.device.nfc.NfcDeviceFactory
import co.ledger.wallet.core.device.usb.UsbDeviceFactory
import co.ledger.wallet.core.utils.Preferenceable

import scala.concurrent.{ExecutionContext, Future}

trait DeviceManager extends Preferenceable {
  import DeviceManager._

  implicit val ec: ExecutionContext

  def compatibleConnectivityTypes: Future[Set[ConnectivityType]] = Future {
    _deviceManager filter {
      case (t, m) => m.isCompatible
    } map {
      case (t, m) => t
    } toSet
  }

  def allCompatibleFactories: Iterable[DeviceFactory] = {
    _deviceManager filter {
      case (t, m) => m.isCompatible
    } map {
      case (t, m) => m
    }
  }

  def deviceFactory(connectivityType: ConnectivityType): DeviceFactory = {
    _deviceManager(connectivityType)
  }

  def requestScan(activity: Activity): ScanRequest = {
    val requests = allCompatibleFactories.map({(factory) =>
      factory.requestScan(activity)
    })
    new CompoundScanRequest(requests)
  }

  def registerDevice(device: Device): Future[UUID] = Future {
    val uuid = UUID.randomUUID()
    device.uuid = uuid
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
    Future.failed(new Exception("Reconnect not implemented yet"))
  }

  def context: Context

  protected[this] val _registeredDevices = scala.collection.mutable.Map[UUID, Device]()

  import DeviceManager.ConnectivityTypes._
  private[this] lazy val _deviceManager = Map[ConnectivityType, DeviceFactory](
    Ble -> new BleDeviceFactory(context, ec),
    Usb -> new UsbDeviceFactory(context, ec),
    Nfc -> new NfcDeviceFactory(context, ec)
  )

  override def PreferencesName: String = "DeviceManager"

  private class CompoundScanRequest(requests: Iterable[ScanRequest]) extends ScanRequest {

    override def start(): Unit = {
      requests foreach {
        _.duration = duration
      }
      super.start()
    }

    override def onStart(): Unit = {
      for (request <- requests) {
        request.start()
      }
    }

    override def onStop(): Unit = {
      for (request <- requests) {
        request.stop()
      }
    }

    for (request <- requests) {
      request.onScanUpdate({
        case DeviceDiscovered(device) => notifyDeviceDiscovered(device)
        case DeviceLost(device) => notifyDeviceLost(device)
      })
    }
  }

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


