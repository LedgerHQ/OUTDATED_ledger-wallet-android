/**
 *
 * FutureExtensions
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 18/11/15.
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

import co.ledger.wallet
import shapeless.Succ

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Promise, Future}
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main
import scala.util.{Failure, Success, Try}

trait FutureExtensions {

  implicit class UiFuture[A](val future: Future[A]) {

    def mapUi[S](f: (A) => S): Future[S] = {
      future.flatMap({(a) =>
        val promise = Promise[S]()
        wallet.common.runOnUiThread({
          promise.complete(Try(f(a)))
        })
        promise.future
      })
    }

    def flatMapUi[S](f: (A) => Future[S]): Future[S] = {
      future.flatMap({(a) =>
        val promise = Promise[S]()
        wallet.common.runOnUiThread({
          promise.completeWith(f(a))
        })
        promise.future
      })
    }

    def onCompleteUi[U](f: (Try[A]) => U): Unit = {
      future.onComplete({ (result) =>
        wallet.common.runOnUiThread({
          f(result)
        })
      })
    }

  }

}

object FutureExtensions {

  def foreach[A, B](items: Array[A])(handler: (A) => Future[B]): Future[Array[B]] = {
    val promise = Promise[Array[B]]()
    val results = new ArrayBuffer[B]()
    def iterate(index: Int): Unit = {
      if (index < items.length) {
        handler(items(index)) onComplete {
          case Success(result) =>
            results += result
            iterate(index + 1)
          case Failure(ex) =>
            promise.failure(ex)
        }
      } else {
        promise.success(results.toArray)
      }
    }
    iterate(0)
    promise.future
  }

}