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
import co.ledger.wallet.wallet.events.PeerGroupEvents.{StartSynchronization, SynchronizationProgress}

import co.ledger.wallet.wallet.events.WalletEvents.AccountCreated

import co.ledger.wallet.wallet.exceptions._
import co.ledger.wallet.wallet.{ExtendedPublicKeyProvider, EarliestTransactionTimeProvider,
Account, Wallet}
import co.ledger.wallet.wallet.DerivationPath.dsl._

import de.greenrobot.event.EventBus
import org.bitcoinj.core._
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.net.discovery.DnsDiscovery
import org.bitcoinj.params.MainNetParams
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
        if (_accounts.length == 0) {
          throw AccountHasNoXpubException(0)
        } else if (_accounts.length == 1 && _persistentState.get.has(FastCatchupTimestamp)) {
          _accounts(0).xpub() flatMap {(xpub) =>
            earliestTransactionTimeProvider.getEarliestTransactionTime(xpub)
          } map {(fastCatchupDate) =>
            _persistentState.get.put(FastCatchupTimestamp, fastCatchupDate.getTime / 1000L)
            save()
            peerGroup
          }
        } else {
          Future.successful(peerGroup)
        }
      } flatMap {(peerGroup) =>
        if (_persistentState.get.has(FastCatchupTimestamp))
          peerGroup.setFastCatchupTimeSecs(_persistentState.get.getLong(FastCatchupTimestamp))
        val promise = Promise[Unit]()
        peerGroup.startBlockChainDownload(new AbstractPeerEventListener {
          override def onBlocksDownloaded(peer: Peer, block: Block, filteredBlock: FilteredBlock, blocksLeft: Int): Unit = {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft)
            // Notify download progression
          }

          override def onTransaction(peer: Peer, t: Transaction): Unit = {
            super.onTransaction(peer, t)
            // If we received a transaction matching a wallet and we have no account + 1, stop
            // synchronizing and wait for an xpub
            /*
            wallets.find({(w) =>
              false
            }).foreach({(_) =>
              peerGroup.stopAsync()
              _peerGroup = None
              promise.failure(AccountHasNoXpubException(_accounts.length))
            })
            */
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
    if (_accountPool.get().contains(index)) {
      _accountPool.get()(index)
    } else {
      val account = new SpvAccountClient(this, index)
     _accountPool.set(_accountPool.get() + (index -> account))
      account
    }
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

  def notifyEmptyAccountIsUsed(): Unit = {
    if (_peerGroup.isEmpty)
      throw new IllegalStateException("Wallet is not initialized")
    val index = _persistentState.get.optInt(AccountCountKey, 0)
    _accounts = _accounts :+ createSpvAccountInstance(index)
    _persistentState.get.put(AccountCountKey, _accounts.length)
    save()
  }

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
        if (_accounts.length == 0 || !_accounts(0).hasXpub)
          throw new AccountHasNoXpubException(0)

        val blockStoreFileExist = blockStoreFile.exists()

        def createPeerGroup(): Future[JPeerGroup] = {
          _peerGroup = Option(new JPeerGroup(networkParameters, blockChain))
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
        blockChain.getChainHead // Will throw if a corruption is detected

        if (!blockStoreFileExist) {
          _accounts(0).xpub() flatMap {(xpub) =>
            earliestTransactionTimeProvider.getEarliestTransactionTime(xpub)
          } flatMap {(time) =>
            val input = context.getAssets.open(Config.CheckpointFilePath)
            CheckpointManager.checkpoint(networkParameters, input, blockStore, time.getTime)
            createPeerGroup()
          } recoverWith {
            case throwable: Throwable => // Complete without checkpoints
              Logger.e("Fail to load checkpoints")
              throwable.printStackTrace()
              createPeerGroup()
          }
        } else {
          createPeerGroup()
        }
      }
    }
  }

  private def init(): Future[Unit] = Future {
    _persistentState.isDefined
  } map { (isInitialized) =>
    if (!isInitialized) {
      if (_walletFile.exists()) {
        val writer = new StringWriter()
        IOUtils.copy(_walletFile, writer)
        _persistentState = Try(new JSONObject(writer.toString)).toOption
      }
      _persistentState = _persistentState.orElse(Some(new JSONObject()))
      Logger.d("Loaded " + _persistentState.get.toString)
      val persistentState = _persistentState.get
      val accountCount = persistentState.optInt(AccountCountKey, 0)

      val accounts = new ArrayBuffer[SpvAccountClient](accountCount)
      for (index <- 0 until accountCount) {
        accounts(index) = createSpvAccountInstance(index)
      }
      _accounts = accounts.toArray
    }
  }

  private def createSpvAccountInstance(index: Int): SpvAccountClient = {
    if (index != _accounts.length)
      throw new IllegalArgumentException(s"Wrong index, expect ${_accounts.length}")
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
      Future.successful(new Date(1434979887 * 1000))
    }
  }

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
