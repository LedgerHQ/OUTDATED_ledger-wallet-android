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

import java.io._
import java.util.Date
import java.util.concurrent.atomic.AtomicReference

import android.content.Context
import co.ledger.wallet.app.Config
import co.ledger.wallet.core.concurrent.SerialQueueTask
import co.ledger.wallet.core.utils.io.IOUtils
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.wallet.DerivationPath.dsl._
import co.ledger.wallet.wallet.events.PeerGroupEvents.{BlockDownloaded, StartSynchronization,
SynchronizationProgress}
import co.ledger.wallet.wallet.events.WalletEvents.AccountCreated
import co.ledger.wallet.wallet.exceptions._
import co.ledger.wallet.wallet.{Account, EarliestTransactionTimeProvider, ExtendedPublicKeyProvider, Wallet}
import de.greenrobot.event.EventBus
import org.bitcoinj.core._
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.store.SPVBlockStore
import org.json.JSONObject

import scala.collection.SortedSet
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success, Try}

class SpvWalletClient(val context: Context, val name: String, val networkParameters: NetworkParameters)
  extends Wallet with SerialQueueTask with Loggable {

  implicit val DisableLogging = false

  type JWallet = org.bitcoinj.core.Wallet
  type JSpvBlockchain = org.bitcoinj.core.BlockChain
  type JPeerGroup = PeerGroup

  def AccountCountKey = "account_count"
  def AccountKey(index: Int) = s"account_$index"
  def FastCatchupTimestamp = "fast_catchup_timeout"

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] = _synchronizationFuture getOrElse {

    def tryAddAccount(index: Int): Future[Unit] = {
      provider.generateXpub(rootDerivationPath/index).flatMap({(xpub) =>
        createSpvAccountInstance(index).xpub(xpub)
      }).flatMap({(_) =>
        eventBus.post(AccountCreated(index))
        downloadBlockChain()
      })
    }

    def downloadBlockChain(): Future[Unit] = {
      var wallets: Seq[JWallet] = null
      Future.sequence((for (account <- _accounts) yield account.load().map({(w: JWallet) => w})).toSeq)
      .flatMap {(w) =>
        wallets = w
        peerGroup()
      } flatMap {(peerGroup) =>
        val promise = Promise[Unit]()
        var maxBlocksLeft: Option[Int] = None
        peerGroup.startBlockChainDownload(new AbstractPeerEventListener {
          override def onBlocksDownloaded(peer: Peer, block: Block, filteredBlock: FilteredBlock, blocksLeft: Int): Unit = {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft)
            // Notify download progression
            if (maxBlocksLeft.isEmpty)
              maxBlocksLeft = Some(blocksLeft)
            eventBus.post(
              SynchronizationProgress(maxBlocksLeft.get - blocksLeft, maxBlocksLeft.get)
            )
            eventBus.post(BlockDownloaded(block))
          }

          override def onTransaction(peer: Peer, t: Transaction): Unit = {
            super.onTransaction(peer, t)
            // If we received a transaction matching a wallet and we have no account + 1, stop
            // synchronizing and wait for an xpub
            val lastAccount  = wallets.last
            if (false || lastAccount.isTransactionRelevant(t)) {
              peerGroup.stopAsync()
              _peerGroup = None
              promise.failure(AccountHasNoXpubException(_accounts.length))
            }
          }
        })
        promise.future
      } recoverWith {
        case AccountHasNoXpubException(index) => tryAddAccount(index)
        case another: Throwable => throw another
      }
    }
    _synchronizationFuture = Some(downloadBlockChain().andThen({
      case Success(_) =>
        _synchronizationFuture = None
      case Failure(error) =>
        error.printStackTrace()
        _synchronizationFuture = None
    }))
    _synchronizationFuture.get
  }

  override def isSynchronizing(): Future[Boolean] = Future {_synchronizationFuture.isDefined}

  def toto = {
    _synchronizationFuture.getOrElse({
      _synchronizationFuture = Some(Future.sequence((for (account <- _accounts) yield account.load().map({(_) => 1})).toSeq) flatMap {(_) =>
        peerGroup()
      } flatMap {(peerGroup) =>
        val promise = Promise[Unit]()
        peerGroup.setFastCatchupTimeSecs(1434979887)

        peerGroup.startBlockChainDownload(new AbstractPeerEventListener () {
          var _max = Int.MaxValue

          override def onChainDownloadStarted(peer: Peer, blocksLeft: Int): Unit = {
            super.onChainDownloadStarted(peer, blocksLeft)
            eventBus.post(StartSynchronization())
          }

          override def onTransaction(peer: Peer, t: Transaction): Unit = {
            super.onTransaction(peer, t)

          }

          override def onBlocksDownloaded(peer: Peer, block: Block, filteredBlock: FilteredBlock,
                                          blocksLeft: Int): Unit = {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft)
            if (_max == Int.MaxValue)
              _max = blocksLeft

            //Logger.d(s"Block downloaded ${block.getTime.toString}")
            if ((_max - blocksLeft) % 100 == 0)
              eventBus.post(SynchronizationProgress(_max - blocksLeft, _max))
          }

        })
        promise.future.map({(_) =>
          _synchronizationFuture = None
        })
      })
      _synchronizationFuture.get
    })
  }

  def rootDerivationPath = 44.h/0.h

  override def accounts(): Future[Array[Account]] = init().map({ (_) =>
    _accounts.asInstanceOf[Array[Account]]
  })

  private val _accountPool = new AtomicReference[Map[Int, SpvAccountClient]](Map())
  override def account(index: Int): Account = {
    /*
    if (_accountPool.get().contains(index)) {
      _accountPool.get()(index)
    } else {
      val account = new SpvAccountClient(this, index)
     _accountPool.set(_accountPool.get() + (index -> account))
      account
    }
    */
    _accounts(index)
  }

  override def transactions(): Future[Set[Transaction]] = accounts().flatMap {(accounts: Array[Account]) =>
    Future sequence  (for (a <- accounts) yield a.transactions()).toSeq
  } map {(groupedTxs) =>
    SortedSet[Transaction](groupedTxs.flatten.toList: _*)(Ordering by {(tx: Transaction) =>
      tx.getUpdateTime
    }).toSet
  }

  override def balance(): Future[Coin] = {
    init().flatMap({ (_) =>
      val promise = Promise[Coin]()
      var sum = Coin.ZERO
      def iter(index: Int): Unit = {
        if (index >= _accounts.length) {
          promise.success(sum)
          return
        }
        _accounts(index).balance().onComplete {
          case Success(balance) =>
            sum = sum add balance
            iter(index + 1)
          case Failure(ex) => promise.failure(ex)
        }
      }
      iter(0)
      promise.future
    })
  }
  override def accountsCount(): Future[Int] = init().map({(_) => _accounts.length})

  def accountPersistedJson(index: Int): Future[JSONObject] = {
    val key: DeterministicKey = null

    init().map({ (_) =>
      if (index >= _accounts.length)
        throw AccountNotFoundException(index)
      var accountJson = _persistentState.get.optJSONObject(AccountKey(index))
      if (accountJson == null) {
        accountJson = new JSONObject()
        _persistentState.get.put(AccountKey(index), accountJson)
      }
      accountJson
    })
  }

  def rootPath(): DeterministicKey = null

  val eventBus =  EventBus
    .builder()
    .throwSubscriberException(true)
    .sendNoSubscriberEvent(false)
    .build()

  def peerGroup(): Future[JPeerGroup] = {
    init() flatMap {(_) =>
      if (_peerGroup.isDefined)
        Future.successful(_peerGroup.get)
      else {
        if (_accounts.length == 0)
          throw new AccountHasNoXpubException(0)

        val blockStoreFileExist = blockStoreFile.exists()

        def createPeerGroup(): Future[JPeerGroup] = {
          _peerGroup = Option(new JPeerGroup(networkParameters, blockChain))
          _peerGroup.get.setFastCatchupTimeSecs(_persistentState.get.getLong(FastCatchupTimestamp))
          _peerGroup.get.setDownloadTxDependencies(false)
          _peerGroup.get.addPeerDiscovery(new DnsDiscovery(networkParameters))
          _peerGroup.get.startAsync()
          _peerGroup.get.addEventListener(new AbstractPeerEventListener {
            override def onPeerConnected(peer: Peer, peerCount: Int): Unit = {
              super.onPeerConnected(peer, peerCount)
              //eventBus.post(s"Peer connected $peerCount")
            }

            override def onPeerDisconnected(peer: Peer, peerCount: Int): Unit = {
              super.onPeerDisconnected(peer, peerCount)
              if (peerCount == 0)
                eventBus.post(s"Peer disconnected $peerCount")
            }
          })
          Future.successful(_peerGroup.get)
        }

        // Create the peer group
        blockChain.getChainHead
        if (!blockStoreFileExist) {
          _accounts(0).xpub() flatMap {(xpub) =>
            earliestTransactionTimeProvider.getEarliestTransactionTime(xpub)
          } flatMap {(time) =>
            _persistentState.get.put(FastCatchupTimestamp, time.getTime / 1000L)
            var input: InputStream = null
            Logger.i(s"Head before checkpoints: ${blockStore.getChainHead.getHeight}")
            val loadCheckpoint = Try {
              input = context.getAssets.open(Config.CheckpointFilePath)
              CheckpointManager.checkpoint(networkParameters, input, blockStore, time.getTime / 1000L)
            }
            Logger.i(s"Head after checkpoints: ${blockStore.getChainHead.getHeight}")
            if (input != null)
              input.close()
            if (loadCheckpoint.isFailure) {
              Logger.e("Fail to load checkpoints")
              loadCheckpoint.failed.get.printStackTrace()
            }
            createPeerGroup()
          }
        } else {
          createPeerGroup()
        }
      }
    }
  }

  private def init(): Future[Unit] = Future {
    if (_persistentState.isEmpty) {
      if (_walletFile.exists()) {
        val writer = new StringWriter()
        IOUtils.copy(_walletFile, writer)
        _persistentState = Try(new JSONObject(writer.toString)).toOption
      }
      _persistentState = _persistentState.orElse(Some(new JSONObject()))
      Logger.d("Loaded " + _persistentState.get.toString)
      val persistentState = _persistentState.get
      val accountCount = persistentState.optInt(AccountCountKey, 0)

      _accounts = Array()
      for (index <- 0 until accountCount) {
        _accounts = _accounts :+ createSpvAccountInstance(index)
      }
    }
  }

  private def createSpvAccountInstance(index: Int): SpvAccountClient = {
    require(index == _accounts.length, s"Wrong index, expect ${_accounts.length} got $index")
    require(_persistentState.isDefined, "Persistent state must be initialized before inflating accounts")
    _accounts = _accounts :+ new SpvAccountClient(this, index)
    _accounts(index)
  }

  def save(): Future[Unit] = Future {
    if (_persistentState.isEmpty) {
      throw new IllegalStateException("Error during save: client is not initialized")
    }
    if (!_walletFile.getParentFile.exists()) {
      _walletFile.getParentFile.mkdirs()
    }
    _persistentState.get.put(AccountCountKey, _accounts.length)
    val input = new StringReader(_persistentState.get.toString)
    IOUtils.copy(input, _walletFile)
    Logger.d("State saved" + _persistentState.get.toString)
  }

  private var _synchronizationFuture: Option[Future[Unit]] = None
  private var _accounts = Array[SpvAccountClient]()
  private var _persistentState: Option[JSONObject] = None
  private var _peerGroup: Option[JPeerGroup] = None
  private def _walletFileName =
    s"${name}_${networkParameters.getAddressHeader}_${networkParameters.getP2SHHeader}"
  private lazy val _walletFile =
    new File(context.getDir("spv_wallets", Context.MODE_PRIVATE), _walletFileName)

  lazy val blockStoreFile =
    new File(context.getDir("blockstores", Context.MODE_PRIVATE), _walletFileName)
  lazy val blockStore = new SPVBlockStore(networkParameters, blockStoreFile)
  lazy val blockChain = new JSpvBlockchain(networkParameters, blockStore)

  lazy val earliestTransactionTimeProvider = new EarliestTransactionTimeProvider {
    override def getEarliestTransactionTime(deterministicKey: DeterministicKey): Future[Date] = {
      // Derive the first 20 addresses from both public and change chain
      Future.successful(new Date(1434979887L * 1000L - (60L * 60L * 24L * 7L)))
    }
  }

}
