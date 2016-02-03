/**
 *
 * QrCodeHelper
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 01/02/16.
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
package co.ledger.wallet.core.utils

import android.graphics.{Color, Bitmap}
import android.net.Uri
import co.ledger.wallet.core.concurrent.ExecutionContext
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter

import scala.concurrent.Future

object QrCodeHelper {

  def encode(text: String, dpWidth: Int, dpHeight: Int): Future[Bitmap] = {
    implicit val ec = ExecutionContext.Implicits.main
    Future {
      val writer = new QRCodeWriter
      val matrix = writer.encode(text, BarcodeFormat.QR_CODE, Convert.dpToPx(dpWidth)
        .toInt, Convert.dpToPx(dpHeight).toInt)
      val width: Int = matrix.getWidth
      val height: Int = matrix.getHeight
      val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
      for (x: Int <- 0 until width) {
        for (y: Int <- 0 until height) {
          val color = if (matrix.get(x, y)) Color.BLACK else Color.WHITE
          bitmap.setPixel(x, y, color)
        }
      }
      bitmap
    }
  }

  def encode(uri: Uri, dpWidth: Int, dpHeight: Int): Future[Bitmap] = encode(uri.toString,
    dpWidth, dpHeight)

}
