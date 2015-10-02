package co.ledger.wallet

import android.app.Activity
import android.content.Context
import android.os.{Handler, Looper}
import co.ledger.wallet.utils.{HexUtils, StringExtensions, JsonUtils, AndroidImplicitConversions}

import scala.concurrent.{Future, Promise}
import scala.util.Try

/**
 *
 * common
 * Default (Template) Project
 *
 * Created by Pierre Pollastri on 17/09/15.
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
package object common extends AndroidImplicitConversions with JsonUtils with StringExtensions with HexUtils {

  private[this] lazy val mainThreadHandler = new Handler(Looper.getMainLooper)
  private[this] lazy val mainThread = Looper.getMainLooper.getThread

  def runOnUiThread(runnable: => Unit): Unit = {
    if (Thread.currentThread == mainThread) {
      runnable
    } else {
      mainThreadHandler.post { runnable }
    }
  }

  def postOnUiThread(runnable: => Unit): Unit = {
    new Handler(Looper.getMainLooper).post(new Runnable {
      override def run(): Unit = {
        runnable
      }
    })
  }

}
