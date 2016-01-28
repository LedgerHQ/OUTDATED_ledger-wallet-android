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

import android.app.PendingIntent
import android.content.{BroadcastReceiver, Context, Intent, IntentFilter}
import android.hardware.usb.{UsbConstants, UsbDevice, UsbEndpoint, UsbManager}
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.Device.{Connect, Disconnect}
import co.ledger.wallet.core.device.DeviceManager.{ConnectivityType, ConnectivityTypes}
import co.ledger.wallet.core.device.usb.UsbDeviceImpl.UsbExchangePerformer
import co.ledger.wallet.core.utils.logs.Loggable
import de.greenrobot.event.EventBus

import scala.concurrent.{ExecutionContext, Future, Promise}

class UsbDeviceImpl(context: Context,
                    override val name: String,
                    usb: UsbDevice,
                    manager: UsbManager)
  extends Device
  with ThreadPoolTask
  with Loggable {

  val ActionUsbPermission = "co.ledger.wallet.ACTION_USB_PERMISSION"
  val VendorID = 0x2581
  val WindUsbProductId = 0x1b7c
  val HidLedgerUsbProductId = 0x3b7c
  val ProtonProductId = 0x4b7c

  override def connect(): Future[Device] = synchronized {
    if (_connectionPromise.isDefined)
      throw new IllegalStateException("Already connected or waiting for connection")
    val intent = new Intent(ActionUsbPermission)
    _connectionPromise = Some(Promise[UsbExchangePerformer]())
    _pendingIntent = Some(PendingIntent.getBroadcast(context, 0, intent, 0))
    val filter = new IntentFilter(ActionUsbPermission)
    context.registerReceiver(_broadcastReceiver, filter)
    manager.requestPermission(usb, _pendingIntent.get)
    _connectionPromise.get.future map {(exchanger) =>
      synchronized {
        _exchanger = Some(exchanger)
        eventBus.post(Connect(this))
        this
      }
    } andThen {
      case all =>
        context.unregisterReceiver(_broadcastReceiver)
        _connectionPromise = None
    }
  }

  override def connectivityType: ConnectivityType = ConnectivityTypes.Usb

  override def debug_=(enable: Boolean): Unit = ???

  override def disconnect(): Unit = {
    _exchanger foreach {(ex) =>
      ex.close()
    }
  }

  override def isDebugEnabled: Boolean = ???

  override def isExchanging: Boolean = _exchanger.exists(_.isExchanging)

  override def isConnected: Boolean = _exchanger.isDefined

  override val eventBus: EventBus = new EventBus()

  @throws[AssertionError]("If there is already an exchange going on")
  override def exchange(command: Array[Byte]): Future[Array[Byte]] = {
    _exchanger match {
      case Some(exchanger) => exchanger.exchange(command)
      case None => Future.failed(new Exception("Not connected"))
    }
  }

  override def readyForExchange: Future[Unit] = {
    _exchanger match {
      case Some(exchanger) => exchanger.readyForExchange
      case None => Future.failed(new Exception("Not connected"))
    }
  }

  override def isConnecting: Boolean = _exchanger.isEmpty && _connectionPromise.isDefined

  override def hashCode(): Int = name.hashCode

  override def equals(o: scala.Any): Boolean = o.isInstanceOf[UsbDeviceImpl] && o.hashCode() == hashCode()

  private def failConnection(cause: Throwable): Unit = {
    if (_connectionPromise.isDefined) {
      _connectionPromise.get.failure(cause)
      onDisconnect()
    }
  }

  private def onDisconnect(): Unit = synchronized {
    if (_connectionPromise.isDefined) {
      _connectionPromise = None
      _exchanger = None
      eventBus.post(Disconnect(this))
    }
  }

  private[this] val _broadcastReceiver = new BroadcastReceiver {
    override def onReceive(context: Context, intent: Intent): Unit = {
      if (_connectionPromise.isDefined) {
        val action = intent.getAction
        if (action == ActionUsbPermission) {
          val device = intent.getParcelableExtra(UsbManager.EXTRA_DEVICE).asInstanceOf[UsbDevice]
          if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
            val dongleInterface = device.getInterface(0)
            var in: Option[UsbEndpoint] = None
            var out: Option[UsbEndpoint] = None
            for (index <- 0 until dongleInterface.getEndpointCount) {
              val endpoint = dongleInterface.getEndpoint(index)
              if (endpoint.getDirection == UsbConstants.USB_DIR_IN) {
                in = Some(endpoint)
              } else {
                out = Some(endpoint)
              }
            }
            val connection = manager.openDevice(device)
            if (connection == null)
              failConnection(new Exception("Unable to open connection"))
            else if (!connection.claimInterface(dongleInterface, true)) {
              failConnection(new Exception("Unable to claim interface"))
            } else {
              val isUsingLedgerTransport =
                (device.getProductId == HidLedgerUsbProductId) || (device.getProductId == ProtonProductId)
              val exchangePerformer = {
                if (device.getProductId == WindUsbProductId) {
                  new UsbWinUsbExchangePerformer(connection, dongleInterface, in.get, out.get)
                } else {
                  new UsbHidExchangePerformer(connection, dongleInterface, in.get, out.get, isUsingLedgerTransport)
                }
              }
              exchangePerformer onDisconnect { () =>
                onDisconnect()
              }
              _connectionPromise.get.success(exchangePerformer)
            }

          } else {
            failConnection(new Exception("Permission not granted"))
          }
        }
      }
    }
  }

  private[this] var _connectionPromise: Option[Promise[UsbExchangePerformer]] = None
  private[this] var _exchanger: Option[UsbExchangePerformer] = None
  private[this] var _pendingIntent: Option[PendingIntent] = None

}

object UsbDeviceImpl {
  trait UsbExchangePerformer {


    def close(): Unit
    def onDisconnect(callback: () => Unit)(implicit ec: ExecutionContext) = {
      _disconnectCallback = Option(callback)
      _disconnectCallbackEC = Option(ec)
    }

    def performExchange(command: Array[Byte]): Array[Byte]

    def exchange(command: Array[Byte])(implicit ec: ExecutionContext): Future[Array[Byte]]
    = synchronized {
      assume(!isExchanging, "There is already an exchange going on.")
      _exchangeFuture = Some(Future {
        performExchange(command)
      } recover {
        case err: Throwable =>
          _exchangeFuture = None
          throw err
      } map {(result) =>
        _exchangeFuture = None
        result
      })
      _exchangeFuture.get
    }

    def readyForExchange(implicit ec: ExecutionContext): Future[Unit] = {
      if (isExchanging) {
        _exchangeFuture.get.map((_) => ())
      } else {
        Future.successful()
      }
    }

    def isExchanging: Boolean = _exchangeFuture.isDefined

    private[this] var _exchangeFuture: Option[Future[Array[Byte]]] = None

    protected def notifyDisconnect(): Unit = {
      _disconnectCallbackEC.foreach {(ec) =>
        ec.execute(new Runnable {
          override def run(): Unit = _disconnectCallback foreach {(callback) =>
            callback()
          }
        })
      }
    }

    protected[this] var _disconnectCallback: Option[() => Unit] = None
    protected[this] var _disconnectCallbackEC: Option[ExecutionContext] = None
  }
}
