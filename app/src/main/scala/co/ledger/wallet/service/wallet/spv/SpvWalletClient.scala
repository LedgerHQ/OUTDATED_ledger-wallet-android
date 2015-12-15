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

import java.util.Date

import android.content.Context
import co.ledger.wallet.app.Config
import co.ledger.wallet.core.concurrent.{AsyncCursor, SerialQueueTask}
import co.ledger.wallet.core.utils.Preferences
import co.ledger.wallet.core.utils.logs.{Logger, Loggable}
import co.ledger.wallet.service.wallet.database.WalletDatabaseOpenHelper
import co.ledger.wallet.wallet.DerivationPath.dsl._
import co.ledger.wallet.wallet._
import co.ledger.wallet.wallet.events.PeerGroupEvents._
import co.ledger.wallet.wallet.events.WalletEvents._
import co.ledger.wallet.wallet.exceptions._
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Wallet => JWallet, _}
import org.bitcoinj.crypto.DeterministicKey

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Promise, Future}

class SpvWalletClient(val context: Context, val name: String, val networkParameters: NetworkParameters)
  extends Wallet with SerialQueueTask with Loggable {

  val NeededAccountIndexKey = "n_index"
  val NeededAccountCreationTimeKey = "n_time"
  val ResumeBlockchainDownloadKey = "max_block_left"

  implicit val DisableLogging = false

  override def account(index: Int): Future[Account] = init() map {(_) => _accounts(index)}

  override def operations(batchSize: Int): Future[AsyncCursor[Operation]] = ???

  override def synchronize(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] =
  Future.successful() flatMap {(_) =>
    if (_syncFuture.isEmpty) {
      _syncFuture = Some(init() flatMap { (appKit) =>
        val promise = Promise[Unit]()
        var _max = _resumeBlockchainDownloadMaxBlock.getOrElse(Int.MaxValue)
        Logger.d("SYNCHRONIZE")
        val lastAccount = _accounts.last
        appKit.synchronize(new DownloadProgressTracker() {

          override def startDownload(blocks: Int): Unit = {
            super.startDownload(blocks)
            Logger.d("START DOWNLOAD")
          }

          override def onBlocksDownloaded(peer: Peer, block: Block, filteredBlock: FilteredBlock,
                                          blocksLeft: Int): Unit = {
            super.onBlocksDownloaded(peer, block, filteredBlock, blocksLeft)
            Future {
              if (_max == Int.MaxValue)
                _max = blocksLeft

              if ((_max - blocksLeft) % 100 == 0)
                eventBus.post(SynchronizationProgress(_max - blocksLeft, _max))
              eventBus.post(BlockDownloaded(block))

              val relevantTx = block.getTransactions.asScala.find(lastAccount.xpubWatcher.isTransactionRelevant)

              if (relevantTx.isDefined) {
                Logger.d("FIND RELEVANT TRANSACTION")
                notifyNewAccountNeed(_accounts.length, block.getTimeSeconds)
                clearAppKit()
                promise.failure(AccountHasNoXpubException(_accounts.length))
              }
            }
          }

          override def doneDownload(): Unit = {
            super.doneDownload()
            promise.success()
          }
        })
        promise.future
      } andThen {
        case all => _syncFuture = None
      })
    }
    _syncFuture.get
  }


  override def accounts(): Future[Array[Account]] = init() map {(_) =>
    _accounts.asInstanceOf[Array[Account]]
  }

  override def isSynchronizing(): Future[Boolean] = Future {
    _syncFuture.isDefined
  }

  override def balance(): Future[Coin] =
    init() flatMap {unit =>
      Future.sequence((for (a <- _accounts) yield a.balance()).toSeq).map { (balances) =>
        balances.reduce(_ add _)
      }
    }

  override def accountsCount(): Future[Int] = init() map {(_) => _accounts.length}

  val eventBus: EventBus = new EventBus()

  val rootPath = 44.h/0.h

  override def setup(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] =
    Future.successful() flatMap {(_) =>
      publicKeyProvider.generateXpub(rootPath/0.h)
    } flatMap {(xpub) =>
      _earliestCreationTimeProvider.getEarliestTransactionTime(xpub) map {(date) =>
        (xpub, date)
      }
    } flatMap {
      case (xpub, date) =>
        val checkpoints = context.getAssets.open(Config.CheckpointFilePath)
        _spvAppKitFactory.setup(Array(xpub), date, checkpoints)
    } map setupWithAppKit map {unit => ()}


  override def needsSetup(): Future[Boolean] = init().map((_) => true).recover {
    case WalletNotSetupException() => false
    case throwable: Throwable => throw throwable
  }

  private def init(): Future[SpvAppKit] = Future.successful() flatMap {(_) =>
    if (_spvAppKit.isEmpty) {
      _spvAppKitFactory.loadFromDatabase().map(setupWithAppKit) recover {
        case NoAppKitToLoadException() => throw WalletNotSetupException()
        case throwable: Throwable =>
          throwable.printStackTrace()
          throw throwable
      }
    } else {
     Future.successful(_spvAppKit.get)
    }
  }

  private def setupWithAppKit(appKit: SpvAppKit): SpvAppKit = {
    _neededAccountIndex foreach { index =>
      if (index >= appKit.accounts.length) {
        appKit.close()
        throw AccountHasNoXpubException(index)
      }
    }
    _spvAppKit = Some(appKit)
    _accounts = appKit.accounts.map((d) => new SpvAccountClient(this, d))
    Logger.d(s"Accounts init ${appKit.accounts.length} ${_accounts.length}")
    appKit
  }

  private def notifyNewAccountNeed(index: Int, creationTimeSeconds: Long): Unit = {
    _preferences.writer
      .putInt(NeededAccountIndexKey, index)
      .putLong(NeededAccountCreationTimeKey, creationTimeSeconds)
      .commit()
    eventBus.post(MissingAccount(index))
  }
  
  private def clearAppKit(): Unit = {
    val appKit = _spvAppKit.get.close()
    _spvAppKit = None
    _accounts.foreach(_.release())
    _accounts = Array()
  }

  private[this] var _accounts = Array[SpvAccountClient]()
  private[this] var _spvAppKit: Option[SpvAppKit] = None
  private[this] lazy val _database = new WalletDatabaseOpenHelper(context, name)
  private[this] lazy val _spvAppKitFactory =
    new SpvAppKitFactory(
      ec,
      networkParameters,
      context.getDir(s"spv_$name", Context.MODE_PRIVATE),
      _database
    )

  private[this] val _preferences = Preferences("SpvWalletClient")(context)
  private[this] var _syncFuture: Option[Future[Unit]] = None
  private[this] lazy val _earliestCreationTimeProvider = new EarliestTransactionTimeProvider {
    override def getEarliestTransactionTime(deterministicKey: DeterministicKey): Future[Date] = {
      // Derive the first 20 addresses from both public and change chain
      Future.successful(new Date(1434979887000L))
    }
  }

  private[this] def _neededAccountIndex = {
    if (_preferences.reader.contains(NeededAccountIndexKey))
      Some(_preferences.reader.getInt(NeededAccountIndexKey, 0))
    else
      None
  }

  private[this] def _neededAccountCreationTime = {
    if (_preferences.reader.contains(NeededAccountCreationTimeKey))
      Some(_preferences.reader.getLong(NeededAccountCreationTimeKey, 0))
    else
      None
  }

  private[this] def _resumeBlockchainDownloadMaxBlock = {
    if (_preferences.reader.contains(ResumeBlockchainDownloadKey))
      Some(_preferences.reader.getInt(ResumeBlockchainDownloadKey, 0))
    else
      None
  }

  private case class OnEmptyAccountReceiveTransactionEvent()

}
