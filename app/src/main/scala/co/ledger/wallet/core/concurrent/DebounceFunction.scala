/**
  *
  * DebouncedFunction
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 11/04/16.
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
package co.ledger.wallet.core.concurrent

import android.os.{Looper, Handler}

import scala.concurrent.{Future, Promise}
import scala.concurrent.duration.Duration
import scala.concurrent.duration._
import scala.util.Try

class DebounceFunction[A, B](wait: Duration, function: (A) => B, handler: Handler) {

  def apply(arg: A): Future[B] = {
    this.synchronized {
      _lastCallback foreach handler.removeCallbacks
      _lastCallback = Some(new Runnable {
        override def run(): Unit = DebounceFunction.this.synchronized {
          _promise.complete(Try(function(arg)))
          _promise = Promise[B]()
        }
      })
      handler.postDelayed(_lastCallback.get, wait.toMillis)
      _promise.future
    }
  }

  private var _promise = Promise[B]()
  private var _lastCallback: Option[Runnable] = None
}

object DebounceFunction {

  val DefaultWaitDuration = 200 milliseconds

  def apply[A, B](function: (A) => B): DebounceFunction[A, B] = {
    this(DefaultWaitDuration)(function)
  }

  def apply[A, B](wait: Duration)(function: (A) => B): DebounceFunction[A, B] = {
    this(wait, new Handler(Looper.getMainLooper))(function)
  }

  def apply[A, B](wait: Duration, handler: Handler)(function: (A) => B): DebounceFunction[A, B] = {
    new DebounceFunction(wait, function, handler)
  }

}