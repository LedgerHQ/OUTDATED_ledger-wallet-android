/**
  *
  * UsbQuickReconnectHandler
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 18/04/16.
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
package co.ledger.wallet.app.reconnect

import java.util.concurrent.CancellationException

import android.app.Activity
import android.app.AlertDialog.Builder
import android.content.DialogInterface
import android.content.DialogInterface.{OnDismissListener, OnClickListener}
import co.ledger.wallet.core.base.DeviceActivity
import co.ledger.wallet.core.device.DeviceFactory.{DeviceDiscovered, ScanRequest}
import co.ledger.wallet.core.device.{Device, DeviceFactory}

import scala.concurrent.{Future, Promise}
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui

import scala.util.{Failure, Success}

class UsbQuickReconnectHandler(activity: DeviceActivity,
                               factory: DeviceFactory,
                               deviceInfo: String) extends QuickReconnectHandler {


  private def startScan(): Unit = {
    _scanRequest = Some(factory.requestScan(activity))
    _scanRequest.get onScanUpdate {
      case DeviceDiscovered(device) =>
        device.matchInfo(deviceInfo) foreach { (isSameDevice) =>
          if (isSameDevice) {
            device.connect() onComplete {
              case Success(_) =>
                activity.registerDevice(device) foreach {_ =>
                  _promise.success(device)
                  cancel()
                }
              case Failure(ex) =>
                _promise.failure(ex)
                cancel()
            }
          }
        }
      case ignore =>
    }
    _scanRequest.get.start()
  }

  override def show(): QuickReconnectHandler = {
    _dialog.show()
    this
  }

  def done(): Unit = {
    _dialog.dismiss()
  }

  override def cancel(): QuickReconnectHandler = {
    _dialog.dismiss()
    _scanRequest.foreach(_.stop())
    if (!_promise.isCompleted) {
      _promise.failure(new CancellationException())
    }
    this
  }

  override def device: Future[Device] = _promise.future
  private val _promise: Promise[Device] = Promise()
  private val _dialog = new Builder(activity)
    .setMessage("Connecting your USB device")
    .setNegativeButton("Cancel", new OnClickListener {
      override def onClick(dialog: DialogInterface, which: Int): Unit = {
        dialog.dismiss()
      }
    })
    .setOnDismissListener(new OnDismissListener {
      override def onDismiss(dialog: DialogInterface): Unit = cancel()
    })
    .create()
  private var _scanRequest: Option[ScanRequest] = None

  startScan()
}
