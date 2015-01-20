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
package com.ledger.ledgerwallet.app.pairing

import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v7.widget.{DefaultItemAnimator, LinearLayoutManager, RecyclerView}
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.ImageButton
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.BaseActivity
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.Toolbar.Style
import com.ledger.ledgerwallet.widget.{DividerItemDecoration, TextView, Toolbar}

class PairedDonglesActivity extends BaseActivity {

  lazy val addPairingButton = TR(R.id.add_pairing_btn).as[ImageButton]
  lazy val pairedDevicesList = TR(R.id.paired_devices_recycler_view).as[RecyclerView]
  private lazy val pairedDevicesAdapter = new PairedDonglesAdapter(this)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.paired_dongles_activity)
    toolbar.setSubtitle(R.string.paired_dongle_waiting_for_an_operation)
    pairedDevicesList.setLayoutManager(new LinearLayoutManager(this))
    pairedDevicesList.setAdapter(pairedDevicesAdapter)
    pairedDevicesList.setHasFixedSize(true)
    pairedDevicesList.setItemAnimator(new DefaultItemAnimator)
    pairedDevicesList.addItemDecoration(new DividerItemDecoration(this, null))

    val l = Array(new PairedDongle("My Ledger Wallet"), new PairedDongle("Sophie's Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"), new PairedDongle("Office Wallet"))
    pairedDevicesAdapter.pairedDongles = l

    addPairingButton setOnClickListener {
      val intent = new Intent(this, classOf[CreateDonglePairingActivity])
      startActivity(intent)
    }

  }

  override def actionBarStyle: Style = Toolbar.Style.Expanded

  class PairedDonglesAdapter(c: Context) extends RecyclerView.Adapter[ViewHolder] {

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
      ui.dongleName.setText(dongle.name)
      ui.pairingDate.setText("Paired on 12/05/2014")
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