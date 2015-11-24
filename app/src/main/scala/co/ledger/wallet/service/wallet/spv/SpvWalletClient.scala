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

import java.io.File
import java.util

import android.content.Context
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.wallet.events.PeerGroupEvents.BlockDownloaded
import co.ledger.wallet.wallet.events.WalletEvents
import co.ledger.wallet.wallet.{Account, Wallet}
import de.greenrobot.event.EventBus
import org.bitcoinj.core._
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.store.{SPVBlockStore, MemoryBlockStore}
import scala.concurrent.{Promise, Future}
import scala.collection.JavaConverters._
import WalletEvents._

class SpvWalletClient(val context: Context, val name: String) extends Wallet with ThreadPoolTask {

  type JWallet = org.bitcoinj.core.Wallet
  type JSpvBlockchain = org.bitcoinj.core.BlockChain
  type JPeerGroup = PeerGroup

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
    Future {
      if (_wallet == null) {
        val params = MainNetParams.get()
        val key = DeterministicKey.deserializeB58("xpub6D4waFVPfPCpRvPkQd9A6n65z3hTp6TvkjnBHG5j2MCKytMuadKgfTUHqwRH77GQqCKTTsUXSZzGYxMGpWpJBdYAYVH75x7yMnwJvra1BUJ", params)

        _wallet = org.bitcoinj.core.Wallet.fromWatchingKey(params, key)
        val file = new File(context.getDir("toto", Context.MODE_PRIVATE), "blockstore")
        if (file.exists())
          file.delete()

        _spvBlockChain = new JSpvBlockchain(params, _wallet, new SPVBlockStore(params, file))
        _peerGroup = new JPeerGroup(params, _spvBlockChain)
        _peerGroup.setDownloadTxDependencies(false)
        _peerGroup.addWallet(_wallet)
        _peerGroup.addPeerDiscovery(new DnsDiscovery(params))
        _wallet.addEventListener(new AbstractWalletEventListener {
          override def onCoinsReceived(wallet: JWallet, tx: Transaction, prevBalance: Coin,
                                       newBalance: Coin): Unit = {
            super.onCoinsReceived(wallet, tx, prevBalance, newBalance)
            eventBus.post(TransactionReceived(tx))
          }
        })
        _peerGroup.startAsync()
        _peerGroup.startBlockChainDownload(new AbstractPeerEventListener() {


          override def onBlocksDownloaded(peer: Peer, block: Block, filteredBlock: FilteredBlock,
                                          blocksLeft: Int): Unit = {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft)
            eventBus.post(BlockDownloaded(blocksLeft))
          }


          override def onPeerConnected(peer: Peer, peerCount: Int): Unit = {
            super.onPeerConnected(peer, peerCount)
            eventBus.post(s"Connected to peer: ${peer.getAddress.getAddr.toString} ${peerCount}")
          }

          override def onChainDownloadStarted(peer: Peer, blocksLeft: Int): Unit = {
            super.onChainDownloadStarted(peer, blocksLeft)
            eventBus.post("Start download blockchain")
          }

          override def onTransaction(peer: Peer, t: Transaction): Unit = {
            super.onTransaction(peer, t)
            eventBus.post(s"Received transaction (O_o) ${t.getHash.toString}")
          }
        })
      }
      promise.success(_wallet)
    } recover {
      case ex => eventBus.post(ex.getMessage)
    }
    promise.future
  }

  private var _wallet: JWallet = null
  private var _spvBlockChain: JSpvBlockchain = null
  private var _peerGroup: JPeerGroup = null
}

object SpvWalletClient {

  def peerGroup(implicit context: Context): PeerGroup = {
    _peerGroup
  }

  // Temporary
  private val _peerGroup = {
    val p = new PeerGroup(MainNetParams.get())
    p.start()

    p
  }

}
