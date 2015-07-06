/**
 *
 * common
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/06/15.
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
package com.ledger.ledgerwallet

import android.app.Activity
import android.content.Context
import android.os.{Looper, Handler}
import com.ledger.ledgerwallet.utils.{JsonUtils, AndroidImplicitConversions}

import scala.concurrent.{Promise, Future, ExecutionContext}
import scala.util.{Failure, Success, Try}
import com.ledger.ledgerwallet.concurrent.ExecutionContext.Implicits.main

// Base on scaloid

package object common extends AndroidImplicitConversions with JsonUtils {

  private[this] lazy val mainThreadHandler = new Handler(Looper.getMainLooper)
  private[this] lazy val mainThread = Looper.getMainLooper.getThread

  def runOnUiThread(runnable: => Unit): Unit = {
    if (Thread.currentThread == mainThread) {
      runnable
    } else {
      mainThreadHandler.post { runnable }
    }
  }

  implicit class UiFuture[+T](f: Future[T]) {

    def thenRunOnUiThread(runnable: (Try[T]) => Unit): Unit = {
      f.onComplete((result: Try[T]) => {
        runOnUiThread {
          runnable(result)
        }
      })
    }

    def contextual(implicit context: Context): Future[T] = {
      val p = Promise[T]()
      f.onComplete((result) => {
        context match {
          case activiy: Activity =>
            if (!activiy.isFinishing && !activiy.isDestroyed)
              p.complete(result)
          case someContext => p.complete(result)
        }
      })
      p.future
    }

  }

}
