/**
 *
 * UsbDeviceFactory
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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.hardware.usb.UsbManager
import android.os.Handler
import co.ledger.wallet.core.device.{Device, DeviceFactory}
import co.ledger.wallet.core.device.DeviceFactory.ScanRequest

import scala.concurrent.{Future, ExecutionContext}

import scala.collection.JavaConverters._

class UsbDeviceFactory(context: Context, executionContext: ExecutionContext) extends DeviceFactory {

  val ScanInterval = 500L

  /** *
    * Check if the android device is compatible with the technology (may block the current thread)
    * @return true if compatible false otherwise
    */
  override def isCompatible: Boolean =
      android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB_MR1 &&
      context.getPackageManager.hasSystemFeature(PackageManager.FEATURE_USB_HOST)

  override def requestScan(activity: Activity): ScanRequest = new ScanRequest {

    private var devices = Array[Device]()
    val handler = new Handler()
    var lastRunnable: Option[Runnable] = None

    override def onStart(): Unit = {
      schedule(true)
    }

    def schedule(now: Boolean): Unit = {
      lastRunnable = Some(new Runnable {
        override def run(): Unit = {
          val deviceList = _service.getDeviceList.asScala map {
            case (name, device) =>
              val d = new UsbDeviceImpl(context, name, device, _service)
              if (!devices.contains(d)) {
                notifyDeviceDiscovered(d)
              }
              (name, d)
          }
          for (device <- devices) {
            if (!deviceList.exists(_._2 == device)) {
              notifyDeviceLost(device)
            }
          }
          devices = deviceList.toArray.map(_._2)
          for ((name, device) <- _service.getDeviceList.asScala) {
            notifyDeviceDiscovered(new UsbDeviceImpl(context, name, device, _service))
          }
          schedule(false)
        }
      })
      if (now)
        handler.post(lastRunnable.get)
      else {
        handler.postDelayed(lastRunnable.get, ScanInterval)
      }
    }

    override def onStop(): Unit = {
      lastRunnable foreach {(runnable) =>
        handler.removeCallbacks(runnable)
      }
    }

  }

  /** *
    * Request the manager required permission
    * @param activity The current activity
    * @return
    */
  override def requestPermission(activity: Activity): Future[Unit] = Future.successful()

  /** *
    * Check if service is enabled (may block the current thread)
    * @return true if enabled false otherwise
    */
  override def isEnabled: Boolean = true

  /** *
    * Check if the manager has enough permissions to run (may block the current thread)
    * @return true if the manager has all required permissions false otherwise
    */
  override def hasPermissions: Boolean = true

  private[this] val _service =  context.getSystemService(Context.USB_SERVICE).asInstanceOf[UsbManager]
}
