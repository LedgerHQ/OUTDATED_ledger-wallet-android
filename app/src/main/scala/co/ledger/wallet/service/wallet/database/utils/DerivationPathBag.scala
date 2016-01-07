/**
 *
 * DerivationPathBag
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 07/01/16.
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

import org.bitcoinj.core.{Transaction, TransactionInput, TransactionOutput, Wallet => JWallet}
import org.bitcoinj.crypto.DeterministicKey

import scala.collection.JavaConverters._
import scala.util.Try

class DerivationPathBag {

  def inflate(tx: Transaction, wallet: JWallet): Unit = {
    for (input <- tx.getInputs.asScala if Try(input.getConnectedOutput.getScriptPubKey.getPubKeyHash).isSuccess) {
      val hash = input.getConnectedOutput.getScriptPubKey.getPubKeyHash
      val key = wallet.getActiveKeychain.findKeyFromPubHash(hash)
      _bag(hash) = key
    }

    for (output <- tx.getOutputs.asScala if output.getScriptPubKey.getPubKeyHash != null) {
      val hash = output.getScriptPubKey.getPubKeyHash
      val key = wallet.getActiveKeychain.findKeyFromPubHash(hash)
      _bag(hash) = key
    }
  }

  def findKey(input: TransactionInput): Option[DeterministicKey] = {
    try
      Option(_bag(input.getConnectedOutput.getScriptPubKey.getPubKeyHash))
    catch {
      case throwable: Throwable => None
    }
  }

  def findKey(output: TransactionOutput): Option[DeterministicKey] = {
    Option(_bag(output.getScriptPubKey.getPubKeyHash))
  }

  private val _bag = scala.collection.mutable.Map[Array[Byte], DeterministicKey]()
}
