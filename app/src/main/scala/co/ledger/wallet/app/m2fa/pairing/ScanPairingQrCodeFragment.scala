/**
 *
 * ScanPairingQrCodeFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/01/15.
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
package co.ledger.wallet.app.m2fa.pairing

import java.util

import android.os.Bundle
import android.view.ViewGroup.LayoutParams
import android.view.{View, ViewGroup, LayoutInflater}
import co.ledger.wallet.R
import co.ledger.wallet.base.{BaseFragment, ContractFragment}
import co.ledger.wallet.utils.TR
import co.ledger.wallet.utils.logs.Logger
import co.ledger.wallet.widget.ScannerFrame

import me.dm7.barcodescanner.zbar.{BarcodeFormat, Result, ZBarScannerView}
import me.dm7.barcodescanner.zbar.ZBarScannerView.ResultHandler
import org.spongycastle.crypto.digests.SHA256Digest
import org.spongycastle.util.encoders.Hex

class ScanPairingQrCodeFragment extends BaseFragment with ContractFragment[CreateDonglePairingActivity.CreateDonglePairingProccessContract] with ResultHandler {

  private lazy val _scannerView = new ZBarScannerView(getActivity)

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    _scannerView.removeViewAt(1)
    _scannerView.addView(new ScannerFrame(getActivity), new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
    _scannerView
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

  override def handleResult(result: Result): Unit = {
    val content = result.getContents
    try {
      val bytes = Hex.decode(content)
      if (bytes.length != 17) return
      val data = bytes.slice(0, 16)
      val chks = bytes(16).toInt
      val validChecksum = new Array[Byte](32)
      val sha256 = new SHA256Digest()
      sha256.update(data, 0, data.length)
      sha256.doFinal(validChecksum, 0)
      if (chks != validChecksum(0).toInt) {
       Logger.d("Invalid checksum")
      }
      contract.setPairingId(content)
    } catch {
      case ex: Throwable => ex.printStackTrace()
    }
  }

  override def tag: String = "ScanPairingQrCodeFragment"
}