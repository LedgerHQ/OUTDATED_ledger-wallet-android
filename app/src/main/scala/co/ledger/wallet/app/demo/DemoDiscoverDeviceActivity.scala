/**
 *
 * DemoDiscoverDeviceActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/16.
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
package co.ledger.wallet.app.demo

import android.content.DialogInterface
import android.os.Bundle
import android.support.v7.app.AlertDialog.Builder
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.View.OnClickListener
import android.view.{View, ViewGroup, LayoutInflater}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{BaseActivity, DeviceActivity}
import co.ledger.wallet.core.device.{DeviceFactory, Device}
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.core.view.{ViewFinder, ViewHolder}
import co.ledger.wallet.core.widget.TextView

import scala.util.{Failure, Success}

class DemoDiscoverDeviceActivity extends BaseActivity
  with DeviceActivity
  with Loggable
  with ViewFinder {
  import DemoDiscoverDeviceActivity._
  import co.ledger.wallet.core.device.DeviceFactory._

  lazy val deviceList: RecyclerView = R.id.devices


  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.demo_discover_device_activity)
    val layoutManager = new LinearLayoutManager(this)
    deviceList.setLayoutManager(layoutManager)
    deviceList.setAdapter(_adapter)
  }

  override def onResume(): Unit = {
    super.onResume()
    startScan()
  }

  override def onPause(): Unit = {
    super.onPause()
    stopScan()
  }

  override def receive: Receive = {
    case all =>
  }

  def startScan(): Unit = {
    if (!_isScanning) {
      _isScanning = true
      def scan(): Unit = {
        deviceManagerService onComplete {
          case Success(service) =>
            val request = service.requestScan()
            request.onScanUpdate(onScanUpdate)
            _scanRequest = Some(request)
            request.duration = DeviceFactory.InfiniteScanDuration
            request.start()
          case Failure(ex) =>
            ex.printStackTrace()
            setResult(ResultError)
            finish()
        }
      }
      scan()
    }
  }

  def onScanUpdate: PartialFunction[ScanUpdate, Unit] = {
    case DeviceDiscovered(device) =>
      Logger.d(s"Find device ${device.name}")
      _adapter.addDevice(device)
    case DeviceLost(device) =>
      Logger.d(s"Lost device ${device.name}")
      _adapter.removeDevice(device)
  }

  def stopScan(): Unit = {
    if (_isScanning) {
      _isScanning = false
      _scanRequest foreach {(request) =>
        _scanRequest = None
        request.stop()
      }
    }
  }

  def connect(device: Device): Unit = {

  }

  private var _scanRequest: Option[ScanRequest] = None
  private var _isScanning = false
  private val _adapter = new DeviceAdapter

  class DeviceAdapter extends RecyclerView.Adapter[DeviceViewHolder] {

    private[this] var _devices = Array[Device]()
    private[this] lazy val _inflater = LayoutInflater.from(DemoDiscoverDeviceActivity.this)

    def addDevice(device: Device): Unit = {
      if (!_devices.contains(device)) {
        _devices = _devices :+ device
        notifyDataSetChanged()
      }
    }

    def removeDevice(device: Device): Unit = {
      if (_devices.contains(device)) {
        _devices = _devices.filter(_ != device)
        notifyDataSetChanged()
      }
    }

    def setDevices(devices: Array[Device]): Unit = {
      _devices = devices
      notifyDataSetChanged()
    }

    override def getItemCount: Int = _devices.length

    override def onBindViewHolder(holder: DeviceViewHolder, position: Int): Unit = {
      holder.update(_devices(position))
    }

    override def onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder = {
      new DeviceViewHolder(_inflater.inflate(R.layout.demo_device_list_item, parent, false))
    }
  }

  class DeviceViewHolder(v: View) extends ViewHolder(v) {

    lazy val name: TextView = R.id.device_name
    lazy val deviceType: TextView = R.id.device_type

    def update(device: Device): Unit = {
      import co.ledger.wallet.core.device.DeviceManager.ConnectivityTypes._
      name.setText(device.name)
      val t = device.connectivityType match {
        case Usb => "USB"
        case Ble => "BLE"
        case Nfc => "NFC"
        case Tee => "TEE"
      }
      deviceType.setText(s"[$t]")
      v.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          new Builder(DemoDiscoverDeviceActivity.this)
            .setTitle("Connect")
            .setMessage("Do you want to connect?")
            .setPositiveButton("yes", new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
                _scanRequest foreach {(request) =>
                  request.stop()
                  _scanRequest = None
                }
                connect(device)
              }
            })
            .setNegativeButton("no", new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = dialog.dismiss()
            })
            .show()
        }
      })
    }

  }

  override implicit def viewId2View[V <: View](id: Int): V = findViewById(id).asInstanceOf[V]
}

object DemoDiscoverDeviceActivity {

  val DiscoveryRequest = 0x01
  val ReconnectRequest = 0x02

  val ResultError = 0xe7707

}