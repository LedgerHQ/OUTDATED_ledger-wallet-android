/**
 *
 * Utxo
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/02/16.
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

import co.ledger.wallet.service.wallet.database.model.OutputRow
import org.bitcoinj.core.{ECKey, Coin, Transaction}

trait Utxo {

  def transaction: Transaction
  def outputIndex: Long
  def path: DerivationPath
  def value: Coin
  def publicKey: Array[Byte]
  def confirmation: Int

}

object Utxo {

  def apply(tx: Transaction, row: OutputRow, confirmation: Int, publicKey: Array[Byte]): Utxo =
    new UtxoImpl(tx, row, confirmation, publicKey)

  private class UtxoImpl(tx: Transaction,
                         row: OutputRow,
                         c: Int,
                         override val publicKey: Array[Byte]) extends Utxo{
    import DerivationPath.dsl._
    override val transaction = tx
    override val outputIndex = row.index
    override val path = 44.h/0.h ++ row.path.get
    override val value = row.value
    override val confirmation = c

  }
}