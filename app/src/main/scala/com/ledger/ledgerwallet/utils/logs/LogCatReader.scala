/**
 *
 * LogCatReader
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 06/07/15.
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
package com.ledger.ledgerwallet.utils.logs

import java.io.{BufferedInputStream, FileOutputStream, File, InputStream}
import java.util.zip.GZIPOutputStream

import android.content.{Intent, Context}
import android.os.Build
import android.support.v4.content.FileProvider
import com.ledger.ledgerwallet.app.Config
import com.ledger.ledgerwallet.content.FileContentProvider
import com.ledger.ledgerwallet.utils.io.IOUtils

import scala.concurrent.{Promise, Future}
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

object LogCatReader {

  def getInputStream: InputStream = Runtime.getRuntime.exec(Array("logcat", "-d")).getInputStream

  def createZipFile(file: File): Future[File] = Future {
    val input = new BufferedInputStream(getInputStream)
    val output = new GZIPOutputStream(new FileOutputStream(file))
    Logger.i(s"Create zip file ${file.getName}")
    IOUtils.copy(input, output)
    Logger.i(s"End zip file ${file.getName} creation")
    output.flush()
    input.close()
    output.close()
    file
  }

  def exportLogsToDefaultTempLogFile()(implicit context: Context): Future[File] = {
    val file = getDefaultTempLogFile
    file.map(createZipFile)
    .getOrElse {
      Promise[File]().failure(file.failed.get).future
    }
  }

  def getDefaultTempLogFile(implicit context: Context) = Try {
    val sharedDirectory = new File(context.getCacheDir, "shared/")
    sharedDirectory.mkdirs()
    File.createTempFile("logs_", ".gzip", sharedDirectory)
  }

  def createEmailIntent(context: Context): Future[Intent] = {
    exportLogsToDefaultTempLogFile()(context).map((f) => {
      val intent = new Intent(Intent.ACTION_SEND)
      intent.setType("application/x-gzip")
      f.setReadable(true, false)
      intent.putExtra(Intent.EXTRA_EMAIL, Array[String](Config.SupportEmailAddress))
      val pkg = context.getPackageManager.getPackageInfo(context.getPackageName, 0)
      val emailBody =
        s"""
          |--- Device informations ---
          | Model: ${Build.MANUFACTURER} ${Build.MODEL} - ${Build.DEVICE}
          | OS: ${Build.DISPLAY}
          | Android version: ${Build.VERSION.RELEASE}
          | Application: ${pkg.packageName}
          | Application version: ${pkg.versionName} - ${pkg.versionCode}
        """.stripMargin
      val uri =  FileProvider.getUriForFile(context, FileContentProvider.Authority, f)
      intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
      intent.putExtra(Intent.EXTRA_TEXT, emailBody)
      intent.putExtra(Intent.EXTRA_STREAM, uri)
      intent
    })
  }

}
