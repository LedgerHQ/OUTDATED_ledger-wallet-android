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

import java.util.UUID

import android.app.Activity
import android.content.{ComponentName, Context, Intent, ServiceConnection}
import android.os.{Bundle, IBinder}
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.event.MainThreadEventReceiver
import co.ledger.wallet.core.utils.Preferenceable
import co.ledger.wallet.service.device.DeviceManagerService

import scala.concurrent.{ExecutionContext, Future, Promise}

trait DeviceActivity extends Activity with MainThreadEventReceiver {

  implicit val ec: ExecutionContext

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
    _deviceManagerServiceConnection foreach {(service) =>
      unbindService(service)
      onDeviceManagerServiceDisconnected()
    }
  }


  abstract override def onDestroy(): Unit = {
    super.onDestroy()
    unbindDeviceManagerService()
  }

  def onDeviceManagerServiceDisconnected(): Unit = {
    _deviceManagerServiceConnection = None
  }

  def connectedDevice: Future[Device] = {
    connectedDeviceUuid match {
      case Some(uuid) =>
        deviceManagerService.flatMap(_.connectedDevice(uuid)).recover {
          case all =>
            throw DeviceActivity.ConnectedDeviceNotFoundException()
        }
      case None => Future.failed(DeviceActivity.ConnectedDeviceNotFoundException())
    }
  }

  def disconnectDevice(): Unit = {
    connectedDeviceUuid match {
      case Some(uuid) =>
        connectedDeviceUuid = null
        deviceManagerService foreach {(manager) =>
          manager.unregisterDevice(uuid)
        }
      case None => // Do nothing
    }

  }

  def connectedDeviceUuid: Option[UUID] = {
    val intentUuid = getIntent.getStringExtra(DeviceActivity.ConnectedDeviceUuid)
    if (intentUuid != null) {
      Some(UUID.fromString(intentUuid))
    } else {
      _connectedDeviceUuid
    }
  }

  def connectedDeviceUuid_=(uuid: UUID) = _connectedDeviceUuid = Option(uuid)

  def registerDevice(device: Device): Future[UUID] = deviceManagerService flatMap {(service) =>
    service.registerDevice(device)
  } map {(uuid) =>
    _connectedDeviceUuid = Some(uuid)
    uuid
  }

  override def startActivity(intent: Intent, options: Bundle): Unit = {
    connectedDeviceUuid match {
      case Some(uuid) =>
        intent.putExtra(DeviceActivity.ConnectedDeviceUuid, uuid.toString)
        super.startActivity(intent, options)
      case None => super.startActivity(intent, options)
    }
  }

  abstract override def onSaveInstanceState(outState: Bundle): Unit = {
    super.onSaveInstanceState(outState)
    if (_connectedDeviceUuid.isDefined) {
      outState.putString(DeviceActivity.ConnectedDeviceUuid, _connectedDeviceUuid.get.toString)
    }
  }

  abstract override def onRestoreInstanceState(savedInstanceState: Bundle): Unit = {
    super.onRestoreInstanceState(savedInstanceState)
    if (savedInstanceState.containsKey(DeviceActivity.ConnectedDeviceUuid)) {
      _connectedDeviceUuid = Some(UUID.fromString(savedInstanceState.getString(DeviceActivity.ConnectedDeviceUuid)))
    }
  }

  private[this] var _deviceManagerServiceConnection: Option[DeviceManagerServiceConnection] = None
  private[this] var _connectedDeviceUuid: Option[UUID] = None

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

object DeviceActivity {
  val ConnectedDeviceUuid = "ConnectedDeviceUuid"

  case class ConnectedDeviceNotFoundException() extends Exception("Not found")

}
