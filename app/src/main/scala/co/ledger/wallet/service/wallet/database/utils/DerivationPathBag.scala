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

import co.ledger.wallet.core.utils.HexUtils
import co.ledger.wallet.service.wallet.database.proxy.{TransactionInputProxy, TransactionOutputProxy, TransactionProxy}
import co.ledger.wallet.wallet.DerivationPath
import org.bitcoinj.core.{Wallet => JWallet, TransactionOutput, TransactionInput, Address}
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.wallet.DeterministicKeyChain

import scala.collection.JavaConverters._

class DerivationPathBag {

  def inflate(tx: TransactionProxy, keyChain: => DeterministicKeyChain, accountIndex: Int): Unit = {
    for (input <- tx.inputs if input.address.isDefined) {
      val hash = new Address(null, input.address.get).getHash160
      val key = keyChain.markPubHashAsUsed(hash)
      if (key != null)
        _bag(HexUtils.bytesToHex(hash)) = (key, accountIndex)
    }

    for (output <- tx.outputs if output.address.isDefined) {
      val hash = new Address(null, output.address.get).getHash160
      val key = keyChain.markPubHashAsUsed(hash)
      if (key != null)
        _bag(HexUtils.bytesToHex(hash)) = (key, accountIndex)
    }
  }

  def findEntry(input: TransactionInput): Option[(DeterministicKey, Int)] = {
    try
      Option(_bag(HexUtils.bytesToHex(input.getConnectedOutput.getScriptPubKey.getPubKeyHash)))
    catch {
      case throwable: Throwable => None
    }
  }

  def findEntry(output: TransactionOutput): Option[(DeterministicKey, Int)] = {
    _bag.lift(HexUtils.bytesToHex(output.getScriptPubKey.getPubKeyHash))
  }

  def findEntry(input: TransactionInputProxy): Option[(DeterministicKey, Int)] = {
    try
      Option(_bag(HexUtils.bytesToHex(new Address(null, input.address.get).getHash160)))
    catch {
      case throwable: Throwable => None
    }
  }

  def findEntry(output: TransactionOutputProxy): Option[(DeterministicKey, Int)] = {
    if (output.address.isDefined)
      _bag.lift(HexUtils.bytesToHex(new Address(null, output.address.get).getHash160))
    else
      None
  }

  def findKey(input: TransactionInputProxy): Option[DeterministicKey] = {
    findEntry(input).map(_._1)
  }

  def findKey(output: TransactionOutputProxy): Option[DeterministicKey] = {
    findEntry(output).map(_._1)
  }

  def findKey(input: TransactionInput): Option[DeterministicKey] = {
    findEntry(input).map(_._1)
  }

  def findKey(output: TransactionOutput): Option[DeterministicKey] = {
    findEntry(output).map(_._1)
  }

  def findPath(intput: TransactionInputProxy): Option[DerivationPath] = {
    findEntry(intput).map(entryToDerivationPath)
  }

  def findPath(output: TransactionOutputProxy): Option[DerivationPath] = {
    findEntry(output).map(entryToDerivationPath)
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

  private val _bag = scala.collection.mutable.Map[String, (DeterministicKey, Int)]()
}
