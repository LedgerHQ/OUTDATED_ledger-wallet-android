/**
 *
 * WalletRef
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 23/11/15.
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

import co.ledger.wallet.app.wallet.WalletPreferences
import co.ledger.wallet.core.concurrent.AsyncCursor
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Transaction, Coin}

import scala.concurrent.Future

trait Wallet {

  def name: String
  def account(index: Int): Future[Account]
  def accounts(): Future[Array[Account]]
  def accountsCount(): Future[Int]
  def balance(): Future[Coin]
  def setup(): Future[Unit]
  def synchronize(): Future[Unit]
  def isSynchronizing(): Future[Boolean]
  def operations(limit: Int = -1, batchSize: Int = Wallet.DefaultOperationsBatchSize): Future[AsyncCursor[Operation]]
  def needsSetup(): Future[Boolean]
  def eventBus: EventBus
  def mostRecentBlock(): Future[Block]
  def pushTransaction(transaction: Transaction): Future[Unit]
  def stop(): Unit
}

object Wallet {
  val DefaultOperationsBatchSize = 20

}