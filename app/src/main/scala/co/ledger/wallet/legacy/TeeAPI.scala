/**
 *
 * TeeAPI
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 31/03/15.
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
package co.ledger.wallet.legacy

import java.io.File

import android.content.Context
import android.os.Build
import co.ledger.wallet.core.net.HttpClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class TeeAPI(context: Context, client: HttpClient = HttpClient.defaultInstance) {

  private[this] var _lastResult: Option[Boolean] = None

  def isDeviceEligible: Future[Boolean] = {
    val p = Promise[Boolean]()
    if (_lastResult.isDefined)
      p.success(_lastResult.get)
    else {
      if (new File("/dev/mobicore").exists() || new File("/dev/mobicore-user").exists()) {
        client.get(s"/mobile/tee/${Build.MODEL}/is_eligible").json.onComplete {
          case Success((json, _)) =>
            val value = Try(json.getBoolean("is_eligible"))
            _lastResult = if (value.isSuccess) Option(value.get) else None
            p.tryComplete(value)
          case Failure(ex) => p.failure(ex)
        }
      } else {
        p.success(false)
      }
    }
    p.future
  }

}

object TeeAPI {

  private[this] var _defaultInstance: TeeAPI = _

  def defaultInstance(implicit context: Context): TeeAPI = {
    if (_defaultInstance == null)
      _defaultInstance = new TeeAPI(context)
    _defaultInstance
  }

}