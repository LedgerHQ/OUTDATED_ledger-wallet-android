/**
  *
  * ApiObjects
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 23/03/16.
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

import java.text.SimpleDateFormat
import java.util.Date

import org.bitcoinj.core.Coin
import org.json.{JSONArray, JSONObject}

import scala.reflect.ClassTag

object ApiObjects {
  /*
      {
       "truncated":false,
       "txs":[
          {
             "hash":"9fdbe15a16fe282291426df15894ab1473e252bc31f244e4d923a17e11743eda",
             "received_at":"2016-03-23T11:54:21Z",
             "lock_time":0,
             "block":{
                "hash":"0000000000000000026aa418ef33e0b079a42d348f35bc0a2fa4bc150a9c459d",
                "height":403912,
                "time":"2016-03-23T11:54:21Z"
             },
             "inputs":[
                {
                   "output_hash":"c74e3063a385d486b2add4b542a7e900c9bd4501a9f37f5e662bdd4b83fd6e02",
                   "output_index":1,
                   "input_index":0,
                   "value":1634001,
                   "address":"1Nd2kJid5fFmPks9KSRpoHQX4VpkPhuATm",
                   "script_signature":"483045022100f72c86ce3bc364c7c45904fa19b44116fc5935f7b25bf9ce290b55dd6e5e2fa602203109dbaf83bfd987c17c5753d3be1b6b6ce9397b66fe04fd72fb299362d122ba01210389e6255cc4c5245d58bb0d074541f392a7e577c004c7529525abe7c3352f77cc"
                }
             ],
             "outputs":[
                {
                   "output_index":0,
                   "value":1000,
                   "address":"1QKJghDW4kLqCsH2pq3XKKsSSeYNPcL5PD",
                   "script_hex":"76a914ffc1247b4de5e6bfd0c632c9f5d74aa2bc9bda5e88ac"
                },
                {
                   "output_index":1,
                   "value":1621651,
                   "address":"19j8biFtMSy5HFRX6mXiurjz3jszg7nLN5",
                   "script_hex":"76a9145fb8d1ce006aeca54d0bbb6355233dcd5885a67f88ac"
                }
             ],
             "fees":11350,
             "amount":1622651
         }
       ]
      }
   */
  class TransactionsAnswer(json: JSONObject) {

    val transactions: Array[Transaction] = {
      inflate(json.getJSONArray("txs")) {
        new Transaction(_)
      }
    }
    def isTruncated: Boolean = json.getBoolean("truncated")

  }

  class Transaction(json: JSONObject) {
    val hash = json.getString("hash")
    val receivedAt = parseJavascriptDate(json.getString("received_at"))
    val lockTime = json.getLong("lock_time")
    val fees = Coin.valueOf(json.getLong("fees"))
    val inputs = {
      inflate(json.getJSONArray("inputs")) {
        new Input(_)
      }
    }
    val outputs = {
      inflate(json.getJSONArray("outputs")) {
        new Output(_)
      }
    }
    val block = {
      if (!json.has("block"))
        None
      else
       Some(new Block(json.getJSONObject("block")))
    }
  }

  class Block(json: JSONObject) {
    val hash = json.getString("hash")
    val height = json.getLong("height")
    val time = parseJavascriptDate(json.getString("time"))
  }

  class Input(json: JSONObject) {
    val previousTxHash = Option(json.optString("output_hash"))
    val outputIndex = json.optLong("output_index")
    val value = {
      if (!json.has("value"))
        None
      else
        Some(Coin.valueOf(json.getLong("value")))
    }
    val address = Option(json.optString("address", null))
    val scriptSig = Option(json.optString("script_signature"))
    val coinbase = Option(json.optString("coinbase"))
  }

  class Output(json: JSONObject) {
    val index = json.getLong("output_index")
    val value = Coin.valueOf(json.getLong("value"))
    val address = Option(json.optString("address"))
    val script = json.getString("script_hex")
  }

  private def inflate[T](json: JSONArray)(map: (JSONObject) => T)(implicit classTag: ClassTag[T]):
  Array[T] = {
    var index = 0
    val result = new Array[T](json.length())
    while (index < result.length) {
      result(index) = map(json.getJSONObject(index))
      index += 1
    }
    result
  }

  private def parseJavascriptDate(jsDate: String): Date = {
    if (jsDate.forall(Character.isDigit))
      new Date(jsDate.toLong * 1000L)
    else
      _dateFormatter.parse(jsDate)
  }
  private val _dateFormatter = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'")
}
