/**
 *
 * ${FILE_NAME}
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/01/15.
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
package com.ledger.ledgerwallet.app.m2fa


import java.text.{SimpleDateFormat, DecimalFormat, NumberFormat}
import java.util.Locale

import android.app.AlertDialog.Builder
import android.app.{AlertDialog, Dialog}
import android.content.DialogInterface.OnClickListener
import android.content.{DialogInterface, Context, Intent}
import android.os.Bundle
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.ImageButton
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.app.m2fa.pairing.CreateDonglePairingActivity
import com.ledger.ledgerwallet.base.{BaseActivity, BigIconAlertDialog}
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.{TextView, Toolbar}

class PairedDonglesActivity extends BaseActivity {

  lazy val addPairingButton = TR(R.id.add_pairing_btn).as[ImageButton]
  lazy val pairedDevicesList = TR(R.id.paired_devices_recycler_view).as[RecyclerView]
  private lazy val pairedDevicesAdapter = new PairedDonglesAdapter(this)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.paired_dongles_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    pairedDevicesList.setLayoutManager(new LinearLayoutManager(this))
    pairedDevicesList.setAdapter(pairedDevicesAdapter)
    pairedDevicesList.setHasFixedSize(true)
    pairedDevicesList.setItemAnimator(new DefaultItemAnimator)

    pairedDevicesAdapter.pairedDongles = PairedDongle.all.sortBy(_.createdAt.get.getTime)

    addPairingButton setOnClickListener {
      val intent = new Intent(this, classOf[CreateDonglePairingActivity])
      startActivityForResult(intent, CreateDonglePairingActivity.CreateDonglePairingRequest)
    }
  }

  override def onActivityResult(requestCode: Int, resultCode: Int, data: Intent): Unit = {
    super.onActivityResult(requestCode, resultCode, data)
    if (requestCode == CreateDonglePairingActivity.CreateDonglePairingRequest) {
      resultCode match {
        case CreateDonglePairingActivity.ResultOk =>
          showSuccessDialog(PairedDongle.all.sortBy(- _.createdAt.get.getTime).head.name.get)
          pairedDevicesAdapter.pairedDongles = PairedDongle.all.sortBy(_.createdAt.get.getTime)
        case CreateDonglePairingActivity.ResultNetworkError => showErrorDialog(R.string.pairing_failure_dialog_error_network)
        case CreateDonglePairingActivity.ResultPairingCancelled => showErrorDialog(R.string.pairing_failure_dialog_cancelled)
        case CreateDonglePairingActivity.ResultWrongChallenge => showErrorDialog(R.string.pairing_failure_dialog_wrong_answer)
        case _ =>
      }
    }
  }

  private[this] def showErrorDialog(contentTextId: Int): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_failure_dialog_title)
      .setContentText(contentTextId)
      .setIcon(R.drawable.ic_big_red_failure)
      .create().show(getSupportFragmentManager, "ErrorDialog")
  }

  private[this] def showSuccessDialog(dongleName: String): Unit = {
    new BigIconAlertDialog.Builder(this)
      .setTitle(R.string.pairing_success_dialog_title)
      .setContentText(TR(R.string.pairing_success_dialog_content).as[String].format(dongleName))
      .setIcon(R.drawable.ic_big_green_success)
      .create().show(getSupportFragmentManager, "SuccessDialog")
  }

  class PairedDonglesAdapter(c: Context) extends RecyclerView.Adapter[ViewHolder] {

    implicit val context = c

    private[this] lazy val DateFormat = android.text.format.DateFormat.getDateFormat(c)

    private var _pairedDongles = Array[PairedDongle]()
    def pairedDongles = _pairedDongles
    def pairedDongles_=(newPairedDongles: Array[PairedDongle]): Unit = {
      _pairedDongles = newPairedDongles
      notifyDataSetChanged()
    }

    lazy val inflater = LayoutInflater.from(c)

    override def onCreateViewHolder(viewgroup: ViewGroup, viewType: Int): ViewHolder = {
      val v = inflater.inflate(R.layout.paired_device_list_item, viewgroup, false)
      new ViewHolder(v)
    }

    override def getItemCount: Int = _pairedDongles.length

    override def onBindViewHolder(ui: ViewHolder, position: Int): Unit = {
      val dongle = pairedDongles(position)
      ui.dongleName.setText(dongle.name.get)
      ui.pairingDate.setText(String.format(TR(R.string.paired_dongle_paired_on).as[String], DateFormat.format(dongle.createdAt.get)))
      ui.deleteButton onClick {
        new Builder(PairedDonglesActivity.this)
        .setMessage(R.string.delete_pairing_dialog_message)
        .setPositiveButton(android.R.string.yes, new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = {
            dongle.delete()
            dialog.dismiss()
            pairedDevicesAdapter.pairedDongles = PairedDongle.all.sortBy(_.createdAt.get.getTime)
            if (pairedDevicesAdapter.pairedDongles.length == 0)
              finish()
          }
        })
        .setNeutralButton(android.R.string.no, new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = dialog.dismiss()
        })
        .create().show()
      }
    }
  }


  class ViewHolder(v: View) extends RecyclerView.ViewHolder(v) {
    lazy val dongleName = TR(v, R.id.dongle_name).as[TextView]
    lazy val pairingDate = TR(v, R.id.pairing_date).as[TextView]
    lazy val deleteButton = TR(v, R.id.delete_btn).as[View]
  }


}

private object PairedDonglesActivity {



}