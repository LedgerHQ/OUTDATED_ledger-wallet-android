/**
 *
 * Benchmark
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/03/15.
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

import co.ledger.wallet.core.utils.logs.Logger

import scala.collection.mutable

object Benchmark {

  private[this] val timers = mutable.Map.empty[String, Long]

  def start(name: String = "Benchmark"): Unit = {
    timers(name) = System.currentTimeMillis()
  }

  def stop(name: String = "Benchmark")(implicit LogTag: String = "Benchmark", DisableLogging: Boolean = false): Unit = {
    if (timers.contains(name))
      Logger.d(s"$name result: ${System.currentTimeMillis() - timers(name)}ms")
  }

  def apply[T](block: => T)(implicit LogTag: String = "Benchmark", DisableLogging: Boolean = false): T = {
    start()
    val result = block
    stop()
    result
  }

}

