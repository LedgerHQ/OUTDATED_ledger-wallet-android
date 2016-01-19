/**
 *
 * BleDeviceImpl
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
package co.ledger.wallet.core.device.ble

import java.util.UUID

import android.bluetooth._
import android.bluetooth.le.ScanResult
import android.content.Context
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.Device.{Connect, Disconnect}
import co.ledger.wallet.core.device.DeviceManager.{ConnectivityTypes, ConnectivityType}
import de.greenrobot.event.EventBus

import scala.collection.JavaConverters._
import scala.concurrent.{Promise, Future}

class BleDeviceImpl(context: Context, scanResult: ScanResult) extends Device with ThreadPoolTask {

  val DefaultServiceUuid = UUID.fromString("D973F2E0-B19E-11E2-9E96-0800200C9A66")
  val DefaultWriteCharacteristicUuid = UUID.fromString("D973F2E2-B19E-11E2-9E96-0800200C9A66")
  val DefaultNotifyCharacteristicUuid = UUID.fromString("D973F2E1-B19E-11E2-9E96-0800200C9A66")
  val ClientCharacteristicConfig = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb")

  override def connect(): Future[Device] = {
    synchronized {
      if (_gatt.isDefined)
        throw new IllegalStateException("Already connected or waiting for connection")
      _gatt = Option(scanResult.getDevice.connectGatt(context, false, _gattCallback))
      _connectionPromise = Some(Promise())
    }
    _connectionPromise.get.future
  }

  override def connectivityType: ConnectivityType = ConnectivityTypes.Ble

  override def disconnect(): Unit = synchronized {
    _gatt.get.disconnect()
    onDisconnect()
  }

  override def isConnected: Boolean = _connectionPromise.isDefined && _connectionPromise.get.isCompleted

  override def exchange(command: Array[Byte]): Future[Array[Byte]] = ???

  override def name: String = scanResult.getDevice.getName

  override def hashCode(): Int = name.hashCode

  override def equals(o: scala.Any): Boolean = o.isInstanceOf[BleDeviceImpl] && o.hashCode() == hashCode()

  override val eventBus = new EventBus()

  override def isConnecting = _connectionPromise.isDefined && !_connectionPromise.get.isCompleted

  private def onDisconnect(): Unit = synchronized {
    if (_connectionPromise.isDefined && !_connectionPromise.get.isCompleted)
      _connectionPromise.get.failure(new Exception("Failed to connect"))
    if (_gatt.isDefined) {
      _connectionPromise = None
      _gatt = None
      eventBus.post(Disconnect(this))
    }
  }

  private[this] var _gatt: Option[BluetoothGatt] = None
  private[this] var _writeCharacteristic: Option[BluetoothGattCharacteristic] = None
  private[this] var _notifyCharacteristic: Option[BluetoothGattCharacteristic] = None
  private[this] var _connectionPromise: Option[Promise[Device]] = None
  private[this] val _gattCallback = new BluetoothGattCallback {

    override def onDescriptorWrite(gatt: BluetoothGatt,
                                   descriptor: BluetoothGattDescriptor,
                                   status: Int): Unit = {
      super.onDescriptorWrite(gatt, descriptor, status)
      if (isConnecting) {
        if (status == BluetoothGatt.GATT_SUCCESS) {
          _connectionPromise.get.success(BleDeviceImpl.this)
          eventBus.post(Connect(BleDeviceImpl.this))
        } else {
          _connectionPromise.get.failure(new Exception("Failed to write notification " +
            "descriptor"))
          onDisconnect()
        }
      }
    }

    override def onReadRemoteRssi(gatt: BluetoothGatt, rssi: Int, status: Int): Unit = {
      super.onReadRemoteRssi(gatt, rssi, status)
    }

    override def onCharacteristicChanged(gatt: BluetoothGatt, characteristic:
    BluetoothGattCharacteristic): Unit = {
      super.onCharacteristicChanged(gatt, characteristic)
    }

    override def onMtuChanged(gatt: BluetoothGatt, mtu: Int, status: Int): Unit = {
      super.onMtuChanged(gatt, mtu, status)
    }

    override def onServicesDiscovered(gatt: BluetoothGatt, status: Int): Unit = {
      super.onServicesDiscovered(gatt, status)
      for (service <- gatt.getServices.asScala) {
        for (characteristic <- service.getCharacteristics.asScala) {
          if (characteristic.getUuid == DefaultWriteCharacteristicUuid) {
            _writeCharacteristic = Some(characteristic)
          } else if (characteristic.getUuid == DefaultNotifyCharacteristicUuid) {
            _notifyCharacteristic = Some(characteristic)
            var hasError = !_gatt.get.setCharacteristicNotification(characteristic, true)
            if (!hasError) {
              val descriptor = characteristic.getDescriptor(ClientCharacteristicConfig)
              descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)
              hasError = !_gatt.get.writeDescriptor(descriptor)
            }
            if (hasError) {
              _connectionPromise.get.failure(new Exception("Failed to set notification " +
                "characteristic"))
              onDisconnect()
            }
          }
        }
      }
    }

    override def onReliableWriteCompleted(gatt: BluetoothGatt, status: Int): Unit = {
      super.onReliableWriteCompleted(gatt, status)
    }

    override def onDescriptorRead(gatt: BluetoothGatt, descriptor: BluetoothGattDescriptor,
                                  status: Int): Unit = {
      super.onDescriptorRead(gatt, descriptor, status)
    }

    override def onCharacteristicWrite(gatt: BluetoothGatt,
                                       characteristic: BluetoothGattCharacteristic,
                                       status: Int): Unit = {
      super.onCharacteristicWrite(gatt, characteristic, status)
    }

    override def onCharacteristicRead(gatt: BluetoothGatt,
                                      characteristic: BluetoothGattCharacteristic,
                                      status: Int): Unit = {
      super.onCharacteristicRead(gatt, characteristic, status)
    }

    override def onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int): Unit = {
      super.onConnectionStateChange(gatt, status, newState)
      newState match {
        case BluetoothProfile.STATE_CONNECTED =>
          if (isConnecting && status == BluetoothGatt.GATT_SUCCESS) {
            _gatt.get.discoverServices()
          } else if (isConnecting) {
            onDisconnect()
          }
        case BluetoothProfile.STATE_DISCONNECTED =>
          onDisconnect()
      }
    }
  }
}
