/**
 *
 * DeviceActivity
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
package co.ledger.wallet.core.base

import android.app.Activity
import android.content.{ComponentName, Context, Intent, ServiceConnection}
import android.os.IBinder
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.service.device.DeviceManagerService

import scala.concurrent.{Promise, Future}

trait DeviceActivity extends Activity with MainThreadEventReceiver {

  def deviceManagerService: Future[DeviceManagerService] = {
    _deviceManagerServiceConnection.getOrElse[DeviceManagerServiceConnection]({
      _deviceManagerServiceConnection = Some(new DeviceManagerServiceConnection)
      bindService(
        new Intent(this, classOf[DeviceManagerService]),
        _deviceManagerServiceConnection.get,
        Context.BIND_AUTO_CREATE
      )
      _deviceManagerServiceConnection.get
    }).future
  }

  def unbindDeviceManagerService(): Unit = {
    _deviceManagerServiceConnection.getOrElse {
      unbindService(_deviceManagerServiceConnection.get)
      onDeviceManagerServiceDisconnected()
    }
  }

  def onDeviceManagerServiceDisconnected(): Unit = {
    _deviceManagerServiceConnection = None
  }

  private[this] var _deviceManagerServiceConnection: Option[DeviceManagerServiceConnection] = None

  class DeviceManagerServiceConnection extends ServiceConnection {
    override def onServiceDisconnected(name: ComponentName): Unit = {
      if (!_promise.isCompleted)
        _promise.failure(new Exception("Unable to bind service"))
      if (_deviceManagerServiceConnection.isDefined)
        onDeviceManagerServiceDisconnected()
    }

    override def onServiceConnected(name: ComponentName, service: IBinder): Unit = {
      _promise.success(service.asInstanceOf[DeviceManagerService#Binder].service)
    }

    def future: Future[DeviceManagerService] = _promise.future

    private[this] val _promise: Promise[DeviceManagerService] = Promise()
  }
}
