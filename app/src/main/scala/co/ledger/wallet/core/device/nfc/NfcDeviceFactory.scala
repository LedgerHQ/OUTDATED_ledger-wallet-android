/**
 *
 * NfcDeviceFactory
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

import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.nfc.tech.IsoDep
import android.nfc.{Tag, NfcManager}
import co.ledger.wallet.core.device.{Device, DeviceFactory}
import co.ledger.wallet.core.device.DeviceFactory.ScanRequest
import nordpol.android.{OnDiscoveredTagListener, TagDispatcher}

import scala.concurrent.{ExecutionContext, Future}

class NfcDeviceFactory(context: Context, executionContext: ExecutionContext) extends DeviceFactory {

  implicit val ec = executionContext

  /** *
    * Check if the android device is compatible with the technology (may block the current thread)
 *
    * @return true if compatible false otherwise
    */
  override def isCompatible: Boolean =
    context.getPackageManager.hasSystemFeature(PackageManager.FEATURE_NFC)

  override def requestScan(activity: Activity): ScanRequest = synchronized {
    val request = new NfcScanRequest(activity)
    if (_currentRequest.isDefined) {
      _currentRequest.foreach(_.stop())
    }
    _currentRequest = Some(request)
    request
  }

  /** *
    * Request the manager required permission
 *
    * @param activity The current activity
    * @return
    */
  override def requestPermission(activity: Activity): Future[Unit] = ???

  /** *
    * Check if service is enabled (may block the current thread)
 *
    * @return true if enabled false otherwise
    */
  override def isEnabled: Boolean = {
    Option(context.getSystemService(Context.NFC_SERVICE).asInstanceOf[NfcManager])
      .map(_.getDefaultAdapter)
      .isDefined
  }

  /** *
    * Check if the manager has enough permissions to run (may block the current thread)
 *
    * @return true if the manager has all required permissions false otherwise
    */
  override def hasPermissions: Boolean = true

  private var _currentRequest: Option[ScanRequest] = None

  private class NfcScanRequest(activity: Activity) extends ScanRequest {

    var lastDevice: Option[Device] = None
    val dispatcher = TagDispatcher.get(activity, new OnDiscoveredTagListener {

      override def tagDiscovered(tag: Tag): Unit = {
        val device = new NfcDeviceImpl(IsoDep.get(tag), ec)
        if (lastDevice.isDefined)
          notifyDeviceLost(lastDevice.get)
        notifyDeviceDiscovered(device)
        lastDevice = Some(device)
      }
    })

    override def onStart(): Unit = {
      dispatcher.enableExclusiveNfc()
    }

    override def onStop(): Unit = NfcDeviceFactory.this.synchronized {
      dispatcher.disableExclusiveNfc()

    }
  }
}
