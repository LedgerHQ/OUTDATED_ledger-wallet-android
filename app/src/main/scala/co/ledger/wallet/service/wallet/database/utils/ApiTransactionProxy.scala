/**
  *
  * ApiTransactionProxy
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
package co.ledger.wallet.service.wallet.database.utils

import java.util.Date

import co.ledger.wallet.service.wallet.api.rest.ApiObjects
import co.ledger.wallet.service.wallet.database.proxy.{TransactionOutputProxy, TransactionInputProxy, TransactionProxy}
import org.bitcoinj.core.Coin

class ApiTransactionProxy(self: ApiObjects.Transaction) extends TransactionProxy {
  override val hash: String = self.hash
  override val block: Option[String] = self.block.map(_.hash)
  override val fees: Option[Coin] = Option(self.fees)
  override def outputs: List[TransactionOutputProxy] = ???
  override val lockTime: Long = self.lockTime
  override val time: Date = self.receivedAt
  override def inputs: List[TransactionInputProxy] = ???

  private class Input(self: ApiObjects.Input) extends TransactionInputProxy {
    override def index: Long = self.outputIndex

    override def isCoinbase: Boolean = self.coinbase.isDefined

    override def address: Option[String] = self.address

    override def scriptSigHex: String = self.scriptSig.get

    override def value: Option[Coin] = self.value

    override def previousTxHash: String = self.previousTxHash.get

    override def coinbase: String = self.coinbase.get
  }

  private class Output(self: ApiObjects.Output) extends TransactionOutputProxy {
    override def index: Long = self.index

    override def address: Option[String] = self.address

    override def value: Coin = self.value

    override def pubKeyScriptHex: String = self.script
  }

}
