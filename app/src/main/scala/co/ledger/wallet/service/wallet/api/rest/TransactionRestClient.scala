/**
  *
  * TransactionRestClient
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

import android.content.Context
import co.ledger.wallet.core.net.{HttpException, HttpClient}
import co.ledger.wallet.service.wallet.api.rest.ApiObjects.TransactionsAnswer
import org.bitcoinj.core.NetworkParameters
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main

import scala.concurrent.Future

class TransactionRestClient(c: Context,
                            networkParameters: NetworkParameters,
                            client: HttpClient = HttpClient.defaultInstance)
  extends RestClient(c, client) {

  def requestSyncToken(): Future[String] = {
    http.get(s"blockchain/v2/$network/syncToken").json map {
      case (json, _) =>
        json.getString("token")
    }
  }

  def deleteSyncToken(token: String): Future[Unit] = {
    http
      .delete(s"blockchain/v2/$network/syncToken")
      .header("X-LedgerWallet-SyncToken" -> token)
      .noResponseBody.map((_) => ())
  }

  def transactions(token: String, addresses: Array[String], blockHash: Option[String]):
  Future[TransactionsAnswer] = {
    http
      .get(s"blockchain/v2/$network/addresses/${addresses.mkString(",")}/transactions")
      .header("X-LedgerWallet-SyncToken" -> token)
      .json map {
        case (json, _) =>
          new TransactionsAnswer(json)
      } recover {
      case ex: HttpException =>
        if (ex.response.statusCode == 404 && blockHash.isDefined) {
          throw BlockNotFoundException(blockHash.get)
        } else {
          throw ex
        }
      case other: Throwable => throw other
    }
  }

  private def network = "btc"

}

case class BlockNotFoundException(blockHash: String) extends Exception(s"Block $blockHash not " +
  s"found (blocks reorg)")