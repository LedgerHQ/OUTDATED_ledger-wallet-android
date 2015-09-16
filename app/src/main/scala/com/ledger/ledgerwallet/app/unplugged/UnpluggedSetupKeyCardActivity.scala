/**
 *
 * UnpluggedSetupKeyCardActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 16/09/15.
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
package com.ledger.ledgerwallet.app.unplugged

import java.util

import android.os.Bundle
import android.widget.RelativeLayout
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.nfc.Utils
import com.ledger.ledgerwallet.utils.TR
import me.dm7.barcodescanner.zbar.ZBarScannerView.ResultHandler
import me.dm7.barcodescanner.zbar.{BarcodeFormat, Result, ZBarScannerView}

import scala.util.Try

class UnpluggedSetupKeyCardActivity extends UnpluggedSetupActivity with ResultHandler {

  private lazy val scannerView = new ZBarScannerView(this)
  private lazy val scannerLayout = TR(R.id.scanner_layout).as[RelativeLayout]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_setup_keycard_activity)
    scannerLayout.addView(scannerView, 0)

    stepNumberTextView.setText(R.string.unplugged_scan_step_number)
    stepInstructionTextView.setText(R.string.unplugged_scan_step_instruction)

    scannerView.removeViewAt(1)
  }

  override def onResume(): Unit = {
    super.onResume()
    val formats = new util.ArrayList[BarcodeFormat](1)
    formats.add(BarcodeFormat.QRCODE)
    scannerView.setResultHandler(this)
    scannerView.setAutoFocus(true)
    scannerView.setFormats(formats)
    scannerView.startCamera()
  }


  override def onPause(): Unit = {
    super.onPause()
    scannerView.stopCamera()
  }

  override def handleResult(result: Result): Unit = {
    if (result.getContents.length == 32 && Try(Utils.decodeHex(result.getContents)).isSuccess) {
      keycardSeed = result.getContents
      startNextActivity(classOf[UnpluggedFinalizeSetupActivity])
    } else {
      scannerView.startCamera()
    }
  }

}
