/**
 *
 * DemoQrCodeScannerActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/02/16.
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

import java.util

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.view.{View, ViewGroup, LayoutInflater}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.BaseActivity
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.view.ViewFinder
import co.ledger.wallet.core.widget.ScannerFrame
import me.dm7.barcodescanner.zbar.ZBarScannerView.ResultHandler
import me.dm7.barcodescanner.zbar.{Result, BarcodeFormat, ZBarScannerView}
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.util.encoders.Hex

class DemoQrCodeScannerActivity extends BaseActivity
  with ViewFinder
  with ResultHandler {
  import DemoQrCodeScannerActivity._
  private lazy val _scannerView = new ZBarScannerView(this)

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    _scannerView.removeViewAt(1)
    _scannerView.addView(new ScannerFrame(this), new LayoutParams(LayoutParams.MATCH_PARENT,
      LayoutParams.MATCH_PARENT))
    setContentView(R.layout.demo_qrcode_scanner_activity)
    findViewById(R.id.camera_preview).asInstanceOf[ViewGroup].addView(_scannerView,
      LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT)
  }

  override def onResume(): Unit = {
    super.onResume()
    val formats = new util.ArrayList[BarcodeFormat](1)
    formats.add(BarcodeFormat.QRCODE)
    _scannerView.setResultHandler(this)
    _scannerView.setAutoFocus(true)
    _scannerView.setFormats(formats)
    _scannerView.startCamera()
  }

  override def onPause(): Unit = {
    super.onPause()
    _scannerView.stopCamera()
  }

  override def handleResult(result: Result): Unit = runOnUiThread {
    val content = result.getContents
    try {
      val uri = Uri.parse(content.replaceFirst(":", ":/"))
      val address = Option(uri.getLastPathSegment)
      val amount = Option(uri.getQueryParameter("amount"))

      val result = new Intent()
      result.putExtra(Address, address.getOrElse(throw new Exception()))
      if (amount.isDefined)
        result.putExtra(Amount, amount.get)
      setResult(Activity.RESULT_OK, result)
      finish()
    } catch {
      case ex: Throwable =>
        ex.printStackTrace()
        _scannerView.startCamera()
    }
  }

  override implicit def viewId2View[V <: View](id: Int): V = findViewById(id).asInstanceOf[V]
}

object DemoQrCodeScannerActivity {

  val Address = "address"
  val Amount = "amount"
}