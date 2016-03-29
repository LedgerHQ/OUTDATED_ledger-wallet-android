/**
  *
  * BitcoinJTransactionProxy
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

import co.ledger.wallet.core.utils.{BitcoinjUtils, HexUtils}
import co.ledger.wallet.service.wallet.database.proxy.{TransactionInputProxy, TransactionOutputProxy, TransactionProxy}
import org.bitcoinj.core.{Coin, Transaction, TransactionInput, TransactionOutput}

import scala.collection.JavaConverters._

class BitcoinJTransactionProxy(self: Transaction) extends TransactionProxy {
  override val hash: String = self.getHashAsString
  override val block: Option[String] = {
    val hashes = self.getAppearsInHashes
    if (hashes == null)
      None
    else
      hashes.asScala.headOption.map(_._1.toString)
  }
  override val fees: Option[Coin] = Option(self.getFee)
  override val outputs: List[TransactionOutputProxy] = {
    var index = 0
    self.getOutputs.asScala.toList map { output =>
      val out = new Output(output, index)
      index += 1
      out
    }
  }
  override val lockTime: Long = self.getLockTime
  override val time: Date = self.getUpdateTime
  override val inputs: List[TransactionInputProxy] = self.getInputs.asScala.toList map {input =>
    new Input(input)
  }

  private class Input(self: TransactionInput) extends TransactionInputProxy {
    override val index: Long = self.getOutpoint.getIndex
    override val isCoinbase: Boolean = self.isCoinBase
    override val address: Option[String] = {
      BitcoinjUtils.toAddress(
        self,
        BitcoinJTransactionProxy.this.self.getParams
      ).map(_.toString)
    }
    override val scriptSigHex: String = HexUtils.bytesToHex(self.getScriptBytes)
    override val value: Option[Coin] = Option(self.getValue)
    override val previousTxHash: String = self.getOutpoint.getHash.toString

    override def coinbase: String = HexUtils.bytesToHex(self.getScriptBytes)
  }

  private class Output(self: TransactionOutput, override val index: Long) extends
    TransactionOutputProxy {
    override def address: Option[String] = {
      BitcoinjUtils.toAddress(self, BitcoinJTransactionProxy.this.self.getParams).map(_.toString)
    }
    override def value: Coin = self.getValue
    override def pubKeyScriptHex: String = HexUtils.bytesToHex(self.getScriptBytes)
  }

}

