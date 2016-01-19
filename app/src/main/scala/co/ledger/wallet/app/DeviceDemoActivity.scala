/**
 *
 * DeviceDemoActivity
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
package co.ledger.wallet.app

import android.app.{ProgressDialog, Dialog}
import android.content.DialogInterface
import android.os.Bundle
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v4.widget.SwipeRefreshLayout.OnRefreshListener
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.support.v7.widget.{LinearLayoutManager, RecyclerView}
import android.view.View.OnClickListener
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.Toast
import co.ledger.wallet.R
import co.ledger.wallet.core.base.{BaseActivity, DeviceActivity}
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.DeviceConnectionManager.{DeviceDiscovered, DeviceLost, ScanUpdate, ScanningRequest}
import co.ledger.wallet.core.device.DeviceManager.ConnectivityTypes
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.view.ViewHolder
import co.ledger.wallet.core.widget.TextView

import scala.util.{Failure, Success}

class DeviceDemoActivity extends BaseActivity with DeviceActivity {

  lazy val refresher = TR(R.id.swipe_refresh_layout).as[SwipeRefreshLayout]
  lazy val devicesList = TR(R.id.devices).as[RecyclerView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.demo_device_activity)
    val layoutManager = new LinearLayoutManager(this)
    devicesList.setLayoutManager(layoutManager)
    refresher.setOnRefreshListener(new OnRefreshListener {
      override def onRefresh(): Unit = scanDevices()
    })
    devicesList.setAdapter(_adapter)
  }

  override def onResume(): Unit = {
    super.onResume()
    scanDevices()
  }

  def connect(device: Device): Unit = {
    val dialog = ProgressDialog.show(this, "Connecting", s"Connecting to ${device.name}")
    device.connect() map { (device) =>
      Toast.makeText(this, s"Connected to ${device.name}", Toast.LENGTH_LONG).show()
    } map {(_) =>
      // Init the API
    } recover {
      case error: Throwable =>
        error.printStackTrace()
        Toast.makeText(this, s"Failed to connect: ${error.getMessage}", Toast.LENGTH_LONG).show()
    } onComplete {(_) =>
      dialog.dismiss()
    }
  }

  private def scanDevices(): Boolean = {
    val empty = _scanRequest.isEmpty
    if (_scanRequest.isEmpty) {
      deviceManagerService map { (service) =>
        _scanRequest = Some(service.deviceManager(ConnectivityTypes.Ble).requestScan())
        _scanRequest.get
      } flatMap { (request) =>
        request.onScanUpdate(onScanDeviceUpdate)
        request.start()
        request.future
      } recover {
        case ex: Throwable =>
          ex.printStackTrace()
          Toast.makeText(this, "Failed to start service", Toast.LENGTH_LONG).show()
          throw ex
      } onComplete {
        case Success(devices) =>
          _adapter.setDevices(devices)
          refresher.setRefreshing(false)
          _scanRequest = None
        case Failure(_) =>
          refresher.setRefreshing(false)
          _scanRequest = None
      }
    }
    empty
  }

  type DeviceUpdateCallback = PartialFunction[ScanUpdate, Unit]
  def onScanDeviceUpdate: DeviceUpdateCallback = {
    case DeviceDiscovered(device) =>
      _adapter.addDevice(device)
    case DeviceLost(device) =>
      _adapter.removeDevice(device)
  }

  override def receive: Receive = {
    case all =>
  }

  private[this] val _adapter = new DeviceAdapter
  private[this] var _scanRequest: Option[ScanningRequest] = None

  class DeviceAdapter extends RecyclerView.Adapter[DeviceViewHolder] {

    private[this] var _devices = Array[Device]()
    private[this] lazy val _inflater = LayoutInflater.from(DeviceDemoActivity.this)

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

    def update(device: Device): Unit = {
      name.setText(device.name)
      v.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = {
          new Builder(DeviceDemoActivity.this)
            .setTitle("Connect")
            .setMessage("Do you want to connect?")
            .setPositiveButton("yes", new DialogInterface.OnClickListener {
              override def onClick(dialog: DialogInterface, which: Int): Unit = {
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


}
