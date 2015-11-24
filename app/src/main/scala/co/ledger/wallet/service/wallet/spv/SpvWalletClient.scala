/**
 *
 * SpvWalletClient
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 24/11/15.
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

import android.content.Context
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.wallet.{Account, Wallet}
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Transaction, Coin}
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.store.MemoryBlockStore
import scala.concurrent.{Promise, Future}
import scala.collection.JavaConverters._

class SpvWalletClient(val context: Context, val name: String) extends Wallet with ThreadPoolTask {

  type JWallet = org.bitcoinj.core.Wallet
  type JSpvBlockchain = org.bitcoinj.core.BlockChain

  override def synchronize(): Future[Unit] = ???

  override def accounts(): Future[Array[Account]] = ???

  override def account(index: Int): Future[Account] = ???

  override def transactions(): Future[Set[Transaction]] = load().map(_.getTransactions(false)
    .asScala.toSet)

  override def balance(): Future[Coin] = ???

  val eventBus =  EventBus
    .builder()
    .throwSubscriberException(true)
    .sendNoSubscriberEvent(false)
    .build()

  /**
   * Temporary implementation
   * @return
   */
  private[this] def load(): Future[JWallet] = {
    val promise = Promise[JWallet]()
    if (_wallet == null) {
      val params = MainNetParams.get()
      val key = DeterministicKey.deserializeB58("xpub6D4waFVPfPCpRvPkQd9A6n65z3hTp6TvkjnBHG5j2MCKytMuadKgfTUHqwRH77GQqCKTTsUXSZzGYxMGpWpJBdYAYVH75x7yMnwJvra1BUJ", params)
      _wallet = org.bitcoinj.core.Wallet.fromWatchingKey(params, key)
      _spvBlockChain = new JSpvBlockchain(params, _wallet,  new MemoryBlockStore(params))
    }
    promise.success(_wallet)
    promise.future
  }

  private var _wallet: JWallet = null
  private var _spvBlockChain: JSpvBlockchain = null
}
