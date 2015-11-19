/**
 *
 * AndroidUtils
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/02/15.
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

import android.content.{Intent, Context}
import android.content.pm.PackageManager
import android.net.Uri
import android.os.{Vibrator, Build}

import scala.util.Try

object AndroidUtils {

  val MarketDetailsUri = Uri.parse("market://details")

  private[this] var _isApplicationInForeground = 0

  def getAppVersion(implicit context: Context)
  : Try[Int] = Try(context.getPackageManager.getPackageInfo(context.getPackageName, 0).versionCode)

  def getModelName: String = {
    val manufacturer = Build.MANUFACTURER
    val model = Build.MODEL
    if (model.startsWith(manufacturer))
      model.capitalize
    else
      manufacturer.capitalize + " " + model
  }

  def notifyActivityOnResume(): Unit = _isApplicationInForeground += 1
  def notifyActivityOnPause(): Unit = _isApplicationInForeground -= 1
  def isApplicationInForeground = _isApplicationInForeground > 0

  def isPackageInstalled(packageName: String)(implicit context: Context): Boolean = {
    Try(context.getPackageManager.getPackageInfo(packageName, PackageManager.GET_ACTIVITIES) != null).getOrElse(false)
  }

  def startMarketApplicationPage(packageName: String)(implicit context: Context): Unit = {
    context.startActivity(new Intent(Intent.ACTION_VIEW,
      MarketDetailsUri.buildUpon()
        .appendQueryParameter("id", packageName)
        .build())
    )
  }

  def startBrowser(uri: Uri)(implicit context: Context): Unit =
    context.startActivity(new Intent(Intent.ACTION_VIEW, uri))

  def vibrate(duration: Long)(implicit context: Context): Unit = {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    if (vibrator.hasVibrator) {
      vibrator.vibrate(duration)
    }
  }

  def vibrate(pattern: Array[Long], repeat: Int = -1)(implicit context: Context): Unit = {
    val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE).asInstanceOf[Vibrator]
    if (vibrator.hasVibrator) {
      vibrator.vibrate(pattern, repeat)
    }
  }

  def hasPermission(permission: String)(implicit context: Context): Boolean = {
    val packageManager = context.getPackageManager
    packageManager.hasSystemFeature(permission)
  }

  def hasNfcFeature()(implicit context: Context): Boolean = hasPermission(PackageManager.FEATURE_NFC)

}
