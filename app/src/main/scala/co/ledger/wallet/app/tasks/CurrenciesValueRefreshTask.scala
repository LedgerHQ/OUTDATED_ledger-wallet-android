/**
  *
  * CurrenciesValueRefreshTask
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 15/04/16.
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
package co.ledger.wallet.app.tasks

import android.content.Context
import android.os.{Looper, Handler}
import co.ledger.wallet.common
import co.ledger.wallet.core.utils.Preferences
import co.ledger.wallet.service.wallet.api.rest.ApiObjects.Currency
import co.ledger.wallet.service.wallet.api.rest.CurrenciesRestClient
import de.greenrobot.event.EventBus
import org.bitcoinj.core.Coin
import org.json.JSONObject

import scala.collection.mutable
import scala.concurrent.duration
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui
import scala.collection.JavaConverters._
import scala.util.Try
import common._

class CurrenciesValueRefreshTask(context: Context) {
  import duration._

  val RefreshInterval = 5 minutes

  def start(): Unit = {
    _handler.post(
      new Runnable {
        override def run(): Unit = {
          if (!_running) {
            _running = true
            _refreshRunnable.run()
          }
        }
      }
    )
  }

  def stop(): Unit = {
    _handler.post(new Runnable {
      override def run(): Unit = {
        if (_running) {
          _running = false
          _handler.removeCallbacks(_refreshRunnable)
        }
      }
    })
  }

  def currencies: Map[String, Currency] = synchronized {
    _currencies
  }

  def isRunning = _running

  val eventBus = new EventBus()

  private def load(): Map[String, Currency] = {
    val prefs = _preferences.reader
    val result = mutable.Map[String, Currency]()
    prefs.getAll.asScala foreach {
      case (key, value: String) =>
        val v = Try(new Currency(new JSONObject(value)))
        if (v.isSuccess)
          result(key) = v.get
      case drop =>
    }
    result.toMap
  }

  private def save(currencies: Map[String, Currency]): Unit = {
    val prefs = _preferences.writer
    prefs.clear()
    currencies foreach {
      case (key, value) =>
        prefs.putString(key, value.toJson.toString())
    }
    prefs.apply()
  }

  private val _preferences = Preferences("currencies")(context)
  private var _currencies: Map[String, Currency] = load()
  private var _running = false
  private val _handler = new Handler(Looper.getMainLooper)
  private val _client = new CurrenciesRestClient(context)
  private val _refreshRunnable: Runnable = new Runnable {
    override def run(): Unit = {
      _client.allCurrencies() foreach {(currencies) =>
        synchronized {
          _currencies = currencies
          save(currencies)
          eventBus.post(CurrenciesValueRefreshTask.CurrenciesValuesRefresh(currencies))
        }
      }
      if (isRunning)
        _handler.postDelayed(_refreshRunnable, RefreshInterval.toMillis)
    }
  }

}

object CurrenciesValueRefreshTask {

  case class CurrenciesValuesRefresh(currencies: Map[String, Currency])

}
