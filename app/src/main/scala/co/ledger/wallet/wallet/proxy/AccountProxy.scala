/**
 *
 * AccountProxy
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/11/15.
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
package co.ledger.wallet.wallet.proxy

import co.ledger.wallet.wallet.{Operation, ExtendedPublicKeyProvider, Account}
import org.bitcoinj.core.{Address, Transaction, Coin}
import org.bitcoinj.crypto.DeterministicKey
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main
import scala.concurrent.Future

class AccountProxy(val wallet: WalletProxy, val index: Int) extends Account {

  override def freshPublicAddress(): Future[Address] = connect().flatMap(_.freshPublicAddress())

  override def synchronize(extendedPublicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] =
    connect().flatMap(_.synchronize(extendedPublicKeyProvider))

  override def xpub(): Future[DeterministicKey] = connect().flatMap(_.xpub())

  override def transactions(): Future[Set[Transaction]] = connect().flatMap(_.transactions())

  override def operations(): Future[Array[Operation]] = connect().flatMap(_.operations())

  override def balance(): Future[Coin] = connect().flatMap(_.balance())

  private def connect(): Future[Account] = {
    wallet.connect().map({(w) =>
      w.account(index)
    })
  }
}
