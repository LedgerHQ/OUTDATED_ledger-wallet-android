/**
 *
 * InstrumentationTestBase
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 17/11/15.
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
package co.ledger.wallet

import junit.framework.Assert

import scala.concurrent.{Promise, Await, Future}
import scala.concurrent.duration.Duration
import scala.util.Try
import scala.concurrent.ExecutionContext.Implicits.global

class InstrumentationTestCase extends android.test.InstrumentationTestCase {

  private[this] var _assertionPromise: Promise[Any] = null

  def await[A](future: Future[A], duration: Duration): A = {
    val promise = Promise[A]()
    _assertionPromise = promise.asInstanceOf[Promise[Any]]
    Await.result(Future.firstCompletedOf(Array(future, promise.future)), duration)
  }

  /** *
    * Ensure assertion to be safely catch by the test thread.
    * @param f The block of code to safely eval
    * @tparam A
    * @return
    */
  def eval[A](f: => A): A = {
      val t = Try(f)
      if (t.isFailure && _assertionPromise != null) {
        if (_assertionPromise != null)
          _assertionPromise.failure(t.failed.get)
        throw t.failed.get
      } else {
        t.get
      }
  }

}
