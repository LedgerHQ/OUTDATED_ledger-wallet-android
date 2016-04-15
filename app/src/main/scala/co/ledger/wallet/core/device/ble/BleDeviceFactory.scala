/**
 *
 * BleDeviceManager
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

import java.util

import android.app.Activity
import android.bluetooth.BluetoothManager
import android.bluetooth.le.{ScanCallback, ScanResult}
import android.content.Context
import android.content.pm.PackageManager
import co.ledger.wallet.core.device.DeviceFactory.ScanRequest
import co.ledger.wallet.core.device.{DeviceFactory, DeviceManager}
import scala.collection.JavaConverters._

import scala.concurrent.{ExecutionContext, Future}

class BleDeviceFactory(context: Context, executionContext: ExecutionContext) extends DeviceFactory {

  implicit val ec = executionContext

  override def isCompatible: Boolean =
    android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN_MR2 &&
    context.getPackageManager.hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)

  override def isEnabled: Boolean = {
    Option(context.getSystemService(Context.BLUETOOTH_SERVICE).asInstanceOf[BluetoothManager])
    .flatMap({(service) =>
      Option(service.getAdapter)
    }).exists(_.isEnabled)
  }

  override def requestPermission(activity: Activity): Future[Unit] = ???

  override def hasPermissions: Boolean = true

  override def requestScan(activity: Activity): ScanRequest = new ScanRequest {

    private[this] var _stop: Option[() => Unit] = None

    override def onStart(): Unit = {
      _adapter foreach {(adapter) =>
        val scanner = adapter.getBluetoothLeScanner
        val callback = new ScanCallback {

          override def onScanResult(callbackType: Int, result: ScanResult): Unit = {
            super.onScanResult(callbackType, result)
            import android.bluetooth.le.ScanSettings._
            val device =  new BleDeviceImpl(context, result)
            //TODO: Find better filtering
            if (device.name != null && device.name.toLowerCase.contains("ledger")) {
              callbackType match {
                case CALLBACK_TYPE_FIRST_MATCH | CALLBACK_TYPE_ALL_MATCHES =>
                  notifyDeviceDiscovered(device)
                case CALLBACK_TYPE_MATCH_LOST =>
                  notifyDeviceLost(device)
              }
            }
          }

          override def onScanFailed(errorCode: Int): Unit = {
            super.onScanFailed(errorCode)
            import DeviceFactory._
            import ScanCallback._
            val ex = errorCode match {
              case SCAN_FAILED_ALREADY_STARTED => ScanAlreadyStartedException()
              case SCAN_FAILED_APPLICATION_REGISTRATION_FAILED =>
                ScanFailedApplicationRegistrationException()
              case SCAN_FAILED_FEATURE_UNSUPPORTED => ScanUnsupportedFeatureException()
              case SCAN_FAILED_INTERNAL_ERROR => ScanInternalErrorException()
            }
            notifyFailure(ex)
          }

          override def onBatchScanResults(results: util.List[ScanResult]): Unit = {
            super.onBatchScanResults(results)
            for (result <- results.asScala) {
              notifyDeviceDiscovered(new BleDeviceImpl(context, result))
            }
          }
        }
        _stop = Some({() =>
          scanner.stopScan(callback)
        })
        scanner.startScan(callback)
      }
    }

    override def onStop(): Unit = {
      _stop.foreach(_())
    }
  }

  private[this] lazy val _adapter = Future {
    if (!isCompatible) {
      throw DeviceManager.AndroidDeviceNotCompatibleException("BLE not compatible")
    }
    if (!hasPermissions) {
      throw DeviceManager.MissingPermissionException("BLE not permitted")
    }
    val service = context.getSystemService(Context.BLUETOOTH_SERVICE).asInstanceOf[BluetoothManager]
    val adapter = service.getAdapter
    if (adapter == null || !adapter.isEnabled) {
      throw DeviceManager.DisabledServiceException("BLE disabled")
    }
    adapter
  }

}
