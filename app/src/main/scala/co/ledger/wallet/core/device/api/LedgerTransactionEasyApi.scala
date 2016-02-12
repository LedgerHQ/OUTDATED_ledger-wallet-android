/**
 *
 * LedgerTransactionEasyApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/02/16.
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
package co.ledger.wallet.core.device.api

import co.ledger.wallet.wallet.{DerivationPath, Utxo}
import org.bitcoinj.core.{Address, Coin, Transaction}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait LedgerTransactionEasyApi extends LedgerTransactionApi {

  def buildTransaction(): TransactionBuilder = new TransactionBuilder

  class TransactionBuilder {

    def sign(): Future[Transaction] = {
      null
    }

    def from(utxo: Array[Utxo]): TransactionBuilder = {
      _utxo ++= utxo
      this
    }

    def from(utxo: Utxo): TransactionBuilder = {
      _utxo += utxo
      this
    }

    def to(address: Address, value: Coin): TransactionBuilder = {
      _to += address -> value
      this
    }

    def fees(fees: Coin): TransactionBuilder = {
      _fees = Option(fees)
      this
    }

    def change(path: DerivationPath): TransactionBuilder = {
      _change = Option(path)
      this
    }

    def onProgress(handler: (Int, Int) => Unit)(implicit ec: ExecutionContext): Unit = {
      _progressHandler = Option(handler -> ec)
    }

    private var _progressHandler: Option[((Int, Int) => Unit, ExecutionContext)] = None
    private var _fees: Option[Coin] = None
    private var _utxo: ArrayBuffer[Utxo] = new ArrayBuffer[Utxo]()
    private var _to: ArrayBuffer[(Address, Coin)] = new ArrayBuffer[(Address, Coin)]()
    private var _change: Option[DerivationPath] = None
  }

}
