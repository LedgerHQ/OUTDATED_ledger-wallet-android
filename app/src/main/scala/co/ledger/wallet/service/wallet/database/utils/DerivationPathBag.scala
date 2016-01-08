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

import co.ledger.wallet.wallet.DerivationPath
import org.bitcoinj.core.{Transaction, TransactionInput, TransactionOutput, Wallet => JWallet}
import org.bitcoinj.crypto.DeterministicKey

import scala.collection.JavaConverters._
import scala.util.Try

class DerivationPathBag {

  def inflate(tx: Transaction, wallet: JWallet, accountIndex: Int): Unit = {
    for (input <- tx.getInputs.asScala if Try(input.getConnectedOutput.getScriptPubKey.getPubKeyHash).isSuccess) {
      val hash = input.getConnectedOutput.getScriptPubKey.getPubKeyHash
      val key = wallet.getActiveKeychain.findKeyFromPubHash(hash)
      if (key != null)
        _bag(hash) = (key, accountIndex)
    }

    for (output <- tx.getOutputs.asScala if output.getScriptPubKey.getPubKeyHash != null) {
      val hash = output.getScriptPubKey.getPubKeyHash
      val key = wallet.getActiveKeychain.findKeyFromPubHash(hash)
      if (key != null)
        _bag(hash) = (key, accountIndex)
    }
  }

  def findEntry(input: TransactionInput): Option[(DeterministicKey, Int)] = {
    try
      Option(_bag(input.getConnectedOutput.getScriptPubKey.getPubKeyHash))
    catch {
      case throwable: Throwable => None
    }
  }

  def findEntry(output: TransactionOutput): Option[(DeterministicKey, Int)] = {
    _bag.lift(output.getScriptPubKey.getPubKeyHash)
  }

  def findKey(input: TransactionInput): Option[DeterministicKey] = {
    findEntry(input).map(_._1)
  }

  def findKey(output: TransactionOutput): Option[DeterministicKey] = {
    findEntry(output).map(_._1)
  }

  def findPath(intput: TransactionInput): Option[DerivationPath] = {
    findEntry(intput).map(entryToDerivationPath)
  }

  def findPath(output: TransactionOutput): Option[DerivationPath] = {
    findEntry(output).map(entryToDerivationPath)
  }

  private def entryToDerivationPath(entry: (DeterministicKey, Int)): DerivationPath = {
    entry match {
      case (key, accountIndex) =>
        import DerivationPath.dsl._
        var path = DerivationPath.Root / accountIndex.h
         for (childNum <- key.getPath.asScala.drop(1)) {
          if (childNum.isHardened)
            path = path / childNum.num().h
          else
            path = path / childNum.num()
        }
        path
    }
  }

  private val _bag = scala.collection.mutable.Map[Array[Byte], (DeterministicKey, Int)]()
}
