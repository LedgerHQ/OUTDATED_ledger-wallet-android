/**
  *
  * proxy
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 29/03/16.
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
package co.ledger.wallet.service.wallet.database

import java.util.Date

import co.ledger.wallet.service.wallet.api.rest.ApiObjects
import co.ledger.wallet.service.wallet.database.utils.{ApiTransactionProxy, BitcoinJTransactionProxy}
import org.bitcoinj.core.{StoredBlock, Coin}

package object proxy {

  trait TransactionProxy {
    def hash: String
    def fees: Option[Coin]
    def time: Date
    def lockTime: Long
    def block: Option[String]
    def inputs: List[TransactionInputProxy]
    def outputs: List[TransactionOutputProxy]
  }

  trait TransactionInputProxy {
    def index: Long
    def value: Option[Coin]
    def previousTxHash: String
    def scriptSigHex: String
    def address: Option[String]
    def isCoinbase: Boolean
    def coinbase: String
  }

  trait TransactionOutputProxy {
    def index: Long
    def address: Option[String]
    def value: Coin
    def pubKeyScriptHex: String
  }

  trait BlockProxy {
    def hash: String
    def height: Int
    def time: Date
  }

  object TransactionProxy {

    def apply(tx: org.bitcoinj.core.Transaction): TransactionProxy = {
      new BitcoinJTransactionProxy(tx)
    }

    def apply(tx: ApiObjects.Transaction): TransactionProxy = {
      new ApiTransactionProxy(tx)
    }

  }

  object BlockProxy {

    def apply(block: StoredBlock): BlockProxy = {
      new BlockProxy {
        override def hash: String = block.getHeader.getHashAsString

        override def height: Int = block.getHeight

        override def time: Date = new Date(block.getHeader.getTimeSeconds * 1000L)
      }
    }

    def apply(block: ApiObjects.Block): BlockProxy = {
      new BlockProxy {
        override def hash: String = block.hash

        override def height: Int = block.height.toInt

        override def time: Date = block.time
      }
    }
  }

}