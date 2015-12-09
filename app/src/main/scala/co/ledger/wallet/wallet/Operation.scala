/**
 *
 * Operation
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 08/12/15.
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
package co.ledger.wallet.wallet

import java.util.Date

import org.bitcoinj.core._
import scala.collection.JavaConverters._

trait Operation {

  def isReception: Boolean
  def isSending: Boolean = !isReception

  def amount: Coin
  def recipients: Array[Address]
  def senders: Array[Address]
  def hash: Sha256Hash
  def date: Date
}

object Operation {

  def fromSendingTransaction(transaction: Transaction, wallet: TransactionBag): Option[Operation] = {
    if (transaction.getValueSentFromMe(wallet).compareTo(Coin.ZERO) != 0)
      Some(new SendingTransactionOperation(transaction, wallet))
    else
      None
  }

  def fromReceptionTransaction(transaction: Transaction, wallet: TransactionBag): Option[Operation] = {
    if (transaction.getValueSentToMe(wallet).compareTo(Coin.ZERO) != 0)
      Some(new ReceptionTransactionOperation(transaction, wallet))
    else
      None
  }

  private class ReceptionTransactionOperation(transaction: Transaction, wallet: TransactionBag)
    extends TransactionOperation(transaction, wallet) {

    override def isReception: Boolean = true

    override def amount: Coin = transaction.getValueSentToMe(wallet)

  }

  private class SendingTransactionOperation(transaction: Transaction, wallet: TransactionBag)
    extends TransactionOperation(transaction, wallet) {

    override def isReception: Boolean = false

    override def amount: Coin = transaction.getValueSentFromMe(wallet)

  }

  private abstract class TransactionOperation(transaction: Transaction, wallet: TransactionBag)
    extends Operation {

    override def senders: Array[Address] = transaction.getInputs.asScala.toArray.map {(input) =>
      if (input.getScriptSig.isSentToAddress)
        input.getScriptSig.getToAddress(input.getParams, true)
      else
        null
    } filter(_ != null)

    override def recipients: Array[Address] = transaction.getOutputs.asScala.toArray.map {(input) =>
      input.getScriptPubKey.getToAddress(input.getParams, true)
    }

    override def hash: Sha256Hash = transaction.getHash

    override def date: Date = transaction.getUpdateTime
  }

}