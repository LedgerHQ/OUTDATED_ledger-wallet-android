/**
  *
  * CurrenciesRestClient
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
package co.ledger.wallet.service.wallet.api.rest

import android.content.Context
import co.ledger.wallet.core.net.HttpClient
import co.ledger.wallet.service.wallet.api.rest.ApiObjects.Currency

import scala.concurrent.Future
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main
class CurrenciesRestClient(c: Context,
                           client: HttpClient = HttpClient.defaultInstance)
  extends RestClient(c, client) {

  def allCurrencies(): Future[Map[String, Currency]] = {
    client.get("currencies/all/exchange_rates").json map {
      case (json, _) =>
        val result = scala.collection.mutable.Map[String, Currency]()
        val keys = json.keys()
        while (keys.hasNext) {
          val key = keys.next()
          result(key) = new Currency(json.getJSONObject(key))
        }
        result.toMap
    }
  }

}
