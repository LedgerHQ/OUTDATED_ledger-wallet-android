/**
 *
 * SpvAccountClient
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
package co.ledger.wallet.service.wallet.spv

import java.util
import java.util.concurrent.Executor

import co.ledger.wallet.core.concurrent.AsyncCursor
import co.ledger.wallet.core.utils.logs.{Logger, Loggable}
import co.ledger.wallet.service.wallet.AbstractDatabaseStoredAccount
import co.ledger.wallet.service.wallet.database.cursor.{BlockCursor, OutputCursor}
import co.ledger.wallet.service.wallet.database.model.{BlockRow, OutputRow, AccountRow}
import co.ledger.wallet.wallet._
import org.bitcoinj.core.Wallet
import org.bitcoinj.core._
import org.bitcoinj.crypto.{ChildNumber, DeterministicKey}
import co.ledger.wallet.wallet.events.WalletEvents._
import org.bitcoinj.wallet.DeterministicKeyChain

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Try

class SpvAccountClient(val wallet: SpvWalletClient, data: (AccountRow, Wallet))
  extends AbstractDatabaseStoredAccount(wallet) with Loggable {

  val row = data._1
  val xpubWatcher = data._2
  val index = row.index

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] =
    wallet.synchronize(provider)

  override def xpub(): Future[DeterministicKey] = Future.successful(xpubWatcher.getWatchingKey)

  def release(): Unit = {
    xpubWatcher.removeEventListener(_eventListener)
  }

  xpubWatcher.addEventListener(_eventListener, new Executor {
    override def execute(command: Runnable): Unit = ec.execute(command)
  })

  override def balance(): Future[Coin] = Future.successful(xpubWatcher.getBalance)

  private[this] lazy val _eventListener = new WalletEventListener

  private class WalletEventListener extends AbstractWalletEventListener {

    override def onCoinsReceived(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance: Coin)
    : Unit = {
      super.onCoinsReceived(w, tx, prevBalance, newBalance)
      Logger.d(s"Receive transaction: ${tx.getHashAsString}")
      wallet.notifyAccountTransaction(SpvAccountClient.this, tx)
    }

    override def onCoinsSent(w: Wallet, tx: Transaction, prevBalance: Coin, newBalance:
    Coin): Unit = {
      super.onCoinsSent(w, tx, prevBalance, newBalance)
      Logger.d(s"Send transaction: ${tx.getHashAsString}")
      wallet.notifyAccountTransaction(SpvAccountClient.this, tx)
    }


    override def onTransactionConfidenceChanged(w: Wallet, tx: Transaction): Unit = {
      super.onTransactionConfidenceChanged(w, tx)
      import TransactionConfidence.ConfidenceType._
      tx.getConfidence.getConfidenceType match {
        case DEAD => // TODO: DELETE OPS
          wallet.notifyDeadTransaction(tx)
        case BUILDING =>
        case PENDING =>
        case UNKNOWN =>
      }
    }

    override def onWalletChanged(w: Wallet): Unit = {
      super.onWalletChanged(w)
    }
  }

  override def rawTransaction(hash: String): Future[Transaction] = Future.successful(xpubWatcher
    .getTransaction(Sha256Hash.wrap(hash)))

  override def keyChain: DeterministicKeyChain = xpubWatcher.getActiveKeychain
}