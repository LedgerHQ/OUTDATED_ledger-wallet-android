/**
 *
 * SpvKit
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/12/15.
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

import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.service.wallet.database.model.AccountRow
import com.google.common.util.concurrent.{ListenableFuture, FutureCallback, Futures}
import org.bitcoinj.core.{Wallet => JWallet, Context, DownloadProgressTracker, PeerGroup,
BlockChain}
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.store.BlockStore
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Promise, Future}
import scala.util.Try

class SpvAppKit(
  val blockStore: BlockStore,
  val blockChain: BlockChain,
  val peerGroup: PeerGroup,
  val accounts: Array[(AccountRow, JWallet)]) extends Loggable {

  peerGroup.setDownloadTxDependencies(true)
  peerGroup.addPeerDiscovery(new DnsDiscovery(Context.get().getParams))

  accounts foreach {
    case (accountRow, wallet) =>
      blockChain.addWallet(wallet)
      peerGroup.addWallet(wallet)
  }

  peerGroup.setFastCatchupTimeSecs(accounts(0)._1.creationTime)

  implicit val DisableLogging = false

  def start(): Future[SpvAppKit] = _startFuture

  def synchronize(tracker: DownloadProgressTracker): Future[Unit] = start() map {(_) =>
    Logger.d("START DOWNLOAD BLOCKCHAIN")("DEBUG", false)
    peerGroup.startBlockChainDownload(tracker)
  }

  def close(): Unit = {
    Logger.d("Closing app kit")
    Logger.d(s"Current chain height ${blockChain.getBestChainHeight}")
    Logger.d(s"Current first account height ${accounts(0)._2.getLastBlockSeenHeight}")
    Try(peerGroup.stop())
    Try(blockStore.close())
  }

  private[this] lazy val _startFuture = {
    val promise = Promise[SpvAppKit]()
    var lastBlockHeight = Int.MaxValue
    accounts foreach {
      case (_, wallet) =>
        if (wallet.getLastBlockSeenHeight < lastBlockHeight)
          lastBlockHeight = wallet.getLastBlockSeenHeight
    }
    blockChain.getChainHead
    Logger.d("Opening app kit")
    Logger.d(s"Current chain height ${blockChain.getBestChainHeight}")
    Logger.d(s"Current first account height ${accounts(0)._2.getLastBlockSeenHeight}")
    Logger.d(s"First account xpub ${accounts(0)._2.getWatchingKey.serializePubB58(MainNetParams
      .get())}")
    Logger.d(s"Creation time ${accounts(0)._2.getEarliestKeyCreationTime}")
    Logger.d(s"Fast catchup ${peerGroup.getFastCatchupTimeSecs}")
    Logger.d("START FUTURE NOW")("DEBUG", false)
    Futures.addCallback(peerGroup.startAsync().asInstanceOf[ListenableFuture[Any]], new FutureCallback[Any] {
      override def onFailure(t: Throwable): Unit = promise.failure(t)

      override def onSuccess(result: Any): Unit = promise.success(SpvAppKit.this)
    })
    promise.future
  }

}

class SpvAppKitBuilder(private val blockStore: BlockStore = null,
                       private val blockChain: BlockChain = null,
                       private val peerGroup: PeerGroup = null,
                       private val accounts: Array[(AccountRow, JWallet)] = Array()) {

  def copy(blockStore: BlockStore = blockStore,
           blockChain: BlockChain = blockChain,
           peerGroup: PeerGroup = peerGroup,
           accounts: Array[(AccountRow, JWallet)] = accounts): SpvAppKitBuilder = {
    new SpvAppKitBuilder(blockStore, blockChain, peerGroup, accounts)
  }

  def withBlockStore(blockStore: BlockStore) = copy(blockStore = blockStore)
  def withBlockChain(blockChain: BlockChain) = copy(blockChain = blockChain)
  def withPeerGroup(peerGroup: PeerGroup) = copy(peerGroup = peerGroup)
  def addAccount(accountRow: AccountRow, wallet: JWallet) = {
    copy(accounts = (accounts :+ (accountRow, wallet)).sortBy(_._1.index))
  }

  def build() = new SpvAppKit(blockStore, blockChain, peerGroup, accounts)

}