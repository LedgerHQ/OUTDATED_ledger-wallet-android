/**
 *
 * SequentialExecutionContext
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/11/15.
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

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.concurrent.{Promise, Future}
import scala.util.Try

class SequentialExecutionContext(ec: scala.concurrent.ExecutionContext) extends scala.concurrent.ExecutionContext {

  private val queue: AtomicReference[Future[Unit]] = new AtomicReference[Future[Unit]](Future.successful())

  override def execute(runnable: Runnable): Unit = {
    val p = Promise[Unit]()

    @tailrec
    def add(): Future[_] = {
      val tail = queue.get()

      if (!queue.compareAndSet(tail, p.future)) {
        add()
      } else {
        tail
      }
    }

    add().onComplete(_ ⇒ p.complete(Try(runnable.run())))(ec)
  }

  override def reportFailure(cause: Throwable): Unit = ec.reportFailure(cause)
}
