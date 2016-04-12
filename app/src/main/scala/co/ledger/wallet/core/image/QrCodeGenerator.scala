/**
  *
  * QrCodeGenerator
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 12/04/16.
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
package co.ledger.wallet.core.image

import java.io.{FileOutputStream, FileInputStream, File}

import android.content.Context
import android.graphics.Bitmap.CompressFormat
import android.graphics.BitmapFactory.Options
import android.graphics.{Color, BitmapFactory, Bitmap}
import android.support.v4.util.LruCache
import co.ledger.wallet.core.crypto.Sha256
import co.ledger.wallet.core.utils.{Convert, HexUtils}
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.jakewharton.disklrucache.DiskLruCache

import scala.concurrent.Future
import scala.util.Success

object QrCodeGenerator {

  implicit val ec = scala.concurrent.ExecutionContext.fromExecutorService(
    java.util.concurrent.Executors.newFixedThreadPool(3)
  )

  def from(context: Context, data: String, dpWidth: Int, dpHeight: Int): Future[Bitmap] = {
    loadFromMemory(context, data, dpWidth, dpHeight)
      .recoverWith {
        case all: Throwable => loadFromCache(context, data,  dpWidth, dpHeight)
      }.recoverWith {
      case all: Throwable =>
        generate(context, data, dpWidth, dpHeight)
    }
  }

  def loadFromMemory(context: Context, data: String, dpWidth: Int, dpHeight: Int): Future[Bitmap] =
  Future {
    val name = dataToFileName(data, dpWidth, dpHeight)
    val result = memoryCache.get(name)
    if (result == null)
      throw new CachedDataNotFound(data)
    result
  }

  def loadFromCache(context: Context, data: String, dpWidth: Int, dpHeight: Int): Future[Bitmap] = Future {
    val name = dataToFileName(data, dpWidth, dpHeight)
    val entry = cache(context).get(name)
    if (entry == null)
      throw new CachedDataNotFound(data)
    val is = entry.getInputStream(0)
    val option = new Options()
    option.inPreferredConfig = Bitmap.Config.RGB_565
    val result = BitmapFactory.decodeStream(is, null, option)
    is.close()
    entry.close()
    result
  }

  def generate(context: Context, data: String,  dpWidth: Int, dpHeight: Int): Future[Bitmap] = Future {
    val writer = new QRCodeWriter
    val matrix = writer.encode(data, BarcodeFormat.QR_CODE, Convert.dpToPx(dpWidth)
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
  } andThen {
    case Success(bitmap) =>
      val name = dataToFileName(data, dpWidth, dpHeight)
      val entry = cache(context).edit(name)
      val os = entry.newOutputStream(0)
      memoryCache.put(name, bitmap)
      bitmap.compress(CompressFormat.PNG, 100, os)
      os.close()
      entry.commit()
    case otherwise => // Do nothing
  }

  private def directory(context: Context): File = {
    new File(context.getFilesDir, "qrcodecache")
  }

  private def cache(context: Context): DiskLruCache = synchronized {
    _cache.getOrElse {
      _cache = Some(DiskLruCache.open(directory(context), 0, 1, 1 * 1024 * 1024))
      _cache.get
    }
  }
  private var _cache: Option[DiskLruCache] = None

  private val memoryCache = new LruCache[String, Bitmap](10)

  private def dataToFileName(data: String, dpWidth: Int, dpHeight: Int): String =
    HexUtils.bytesToHex(Sha256.digest(s"$data $dpWidth $dpHeight".getBytes)).toLowerCase

  class CachedDataNotFound(data: String) extends Exception(s"Not found '$data'")

}
