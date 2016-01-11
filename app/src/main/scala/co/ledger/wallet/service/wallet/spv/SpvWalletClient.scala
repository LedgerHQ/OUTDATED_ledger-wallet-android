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

import java.util
import java.util.Date

import android.content.Context
import co.ledger.wallet.app.Config
import co.ledger.wallet.core.concurrent.{AsyncCursor, SerialQueueTask}
import co.ledger.wallet.core.utils.{BitcoinjUtils, Preferences}
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.service.wallet.database.DatabaseStructure.OperationTableColumns
import co.ledger.wallet.service.wallet.database.utils.DerivationPathBag
import co.ledger.wallet.service.wallet.database.{WalletDatabaseOpenHelper, WalletDatabaseWriter}
import co.ledger.wallet.wallet.DerivationPath.dsl._
import co.ledger.wallet.wallet._
import co.ledger.wallet.wallet.events.PeerGroupEvents._
import co.ledger.wallet.wallet.events.WalletEvents._
import co.ledger.wallet.wallet.exceptions._
import de.greenrobot.event.EventBus
import org.bitcoinj.core.AbstractBlockChain.NewBlockType
import org.bitcoinj.core.{Wallet => JWallet, _}
import org.bitcoinj.crypto.DeterministicKey

import scala.collection.JavaConverters._
import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{Future, Promise}

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
      _syncFuture = Some(performSynchronization(publicKeyProvider))
    }
    _syncFuture.get
  }

  private[this] def performSynchronization(extendedPublicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = {
    init() flatMap { (appKit) =>
      val promise = Promise[Unit]()
      var _max = _resumeBlockchainDownloadMaxBlock.getOrElse(Int.MaxValue)
      Logger.d("SYNCHRONIZE")
      val accountsCount = _accounts.length
      val eventHandler = new Object {
        def onEvent(event: NeedNewAccount): Unit = {
          if (!promise.isCompleted)
            promise.failure(AccountHasNoXpubException(accountsCount))
        }
      }
      _internalEventBus.register(eventHandler)
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
          }
        }

        override def doneDownload(): Unit = {
          super.doneDownload()
          promise.success()
        }
      })
      promise.future andThen {
        case all =>
          _internalEventBus.unregister(eventBus)
      }
    } andThen {
      case all => _syncFuture = None
    } recoverWith {
      case AccountHasNoXpubException(index) =>
        Logger.d(s"Need $index account")
        extendedPublicKeyProvider.generateXpub(rootPath/index).flatMap {(xpub) =>
          _database.writer.createAccountRow(
            index = Some(index),
            xpub58 = Some(xpub.serializePubB58(networkParameters)),
            creationTime = _neededAccountCreationTime
          )
          performSynchronization(extendedPublicKeyProvider)
        } recover {
          case all => throw AccountHasNoXpubException(index)
        }
    }
  }

  def notifyAccountReception(account: SpvAccountClient, tx: Transaction, newBalance: Coin): Unit = Future {
    if (_accounts.length > 0 && account == _accounts.last) {
      notifyNewAccountNeed(account.index + 1, tx.getUpdateTime.getTime / 1000)
    }
    pushTransaction(account, tx)
    eventBus.post(CoinReceived(account.index, newBalance))
  } recover {
    case throwable: Throwable => throwable.printStackTrace()
  }

  def notifyAccountSend(account: SpvAccountClient, tx: Transaction, newBalance: Coin): Unit = Future {
    if (_accounts.length > 0 && account == _accounts.last) {
      notifyNewAccountNeed(account.index + 1, tx.getUpdateTime.getTime / 1000)
    }
    pushTransaction(account, tx)
    eventBus.post(CoinSent(account.index, newBalance))
  } recover {
      case throwable: Throwable => throwable.printStackTrace()
    }

  private def pushTransaction(account: SpvAccountClient, tx: Transaction): Unit = {
    val writer = _database.writer
    writer.beginTransaction()
    val bag = new DerivationPathBag
    try {
      bag.inflate(tx, account.xpubWatcher, account.index)
      writer.updateOrCreateTransaction(tx, bag)
      // Create operation now
      // Create receive send operation
      val walletTransaction = new WalletTransaction(account.xpubWatcher, tx, bag)
      val isSend = computeSendOperation(account, walletTransaction, writer)
      computeReceiveOperation(account, walletTransaction, !isSend, writer)
      writer.commitTransaction()
    } catch {
      case throwable: Throwable => throwable.printStackTrace()
    }
    writer.endTransaction()
  }

  private def computeSendOperation(account: Account,
                                   transaction: WalletTransaction,
                                   writer: WalletDatabaseWriter): Boolean = {
    if (transaction.containsWalletInputs) {
      val value: Coin = transaction.sendValue
      writer.updateOrCreateOperation(
        account.index,
        transaction.tx.getHashAsString,
        OperationTableColumns.Types.Send,
        value.getValue,
        transaction.senders,
        transaction.recipients)
      true
    } else {
      false
    }
  }

  private def computeReceiveOperation(account: Account,
                                      transaction: WalletTransaction,
                                      forceReception: Boolean,
                                      writer: WalletDatabaseWriter): Boolean = {
    if (forceReception || transaction.numberOfPublicOutput > 0) {
      val value: Coin = transaction.receivedValue
      writer.updateOrCreateOperation(
        account.index,
        transaction.tx.getHashAsString,
        OperationTableColumns.Types.Reception,
        value.getValue,
        transaction.senders,
        transaction.recipients)
      true
    } else {
      false
    }
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
    appKit.blockChain.addListener(new BlockChainListener {
      override def reorganize(splitPoint: StoredBlock, oldBlocks: util.List[StoredBlock],
                              newBlocks: util.List[StoredBlock]): Unit = {
        // TODO: Handle block reorgs
      }

      override def notifyNewBestBlock(block: StoredBlock): Unit = Future {
        _database.writer.updateOrCreateBlock(block)
      }

      override def notifyTransactionIsInBlock(txHash: Sha256Hash, block: StoredBlock, blockType:
      NewBlockType, relativityOffset: Int): Boolean = false

      override def receiveFromBlock(tx: Transaction, block: StoredBlock, blockType: NewBlockType,
                                    relativityOffset: Int): Unit = {}

      override def isTransactionRelevant(tx: Transaction): Boolean = false
    })
    _accounts = appKit.accounts.map((d) => new SpvAccountClient(this, d))
    Logger.d(s"Accounts init ${appKit.accounts.length} ${_accounts.length}")
    appKit
  }

  private def notifyNewAccountNeed(index: Int, creationTimeSeconds: Long): Unit = {
    _preferences.writer
      .putInt(NeededAccountIndexKey, index)
      .putLong(NeededAccountCreationTimeKey, creationTimeSeconds)
      .commit()
    clearAppKit()
    _internalEventBus.post(NeedNewAccount())
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
  private[this] val _internalEventBus = new EventBus()
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
  private case class NeedNewAccount()

  private class WalletTransaction(val wallet: JWallet, val tx: Transaction, bag: DerivationPathBag) {

    lazy val containsWalletInputs: Boolean = {
     inputs.exists(_.isMine)
    }

    lazy val inputs = tx.getInputs.asScala.map({(input) =>
      new WalletTransactionInput(input, bag.findPath(input))
    })

    lazy val outputs = tx.getOutputs.asScala.map({(output) =>
      new WalletTransactionOutput(output, bag.findPath(output))
    })

    lazy val numberOfChangeOutput = outputs.count(_.isOnInternalBranch)

    lazy val numberOfPublicOutput = outputs.count(_.isOnExternalBranch)

    lazy val senders = {
      val senders = new ArrayBuffer[String](inputs.length)
      for (input <- inputs) {
        val address = BitcoinjUtils.toAddress(input.input, networkParameters)
        if (address.isDefined)
          senders += address.get.toString
        else if (input.input.isCoinBase)
          senders += "coinbase"
      }
      senders.toArray
    }

    lazy val recipients = {
      val recipients = ArrayBuffer[String]()
      for (output <- outputs) {
        BitcoinjUtils.toAddress(output.output, networkParameters).map(_.toString).foreach({(a) =>
          recipients += a
        })
      }
      recipients.toArray
    }

    def sendValue: Coin = {
      var value = Coin.ZERO
      for (input <- inputs if input.isMine) {
        value = value.add(input.value)
      }
      outputs.filter(_.isOnInternalBranch).foreach({output =>
        value = value subtract output.value
      })
      value
    }

    def receivedValue: Coin = {
      val walletOutputs = outputs.filter(_.isMine)
      var value = Coin.ZERO
      for (output <- walletOutputs) {
        if (output.isOnExternalBranch || !containsWalletInputs)
          value = value.add(output.value)
      }
      value
    }

    class WalletTransactionInput(val input: TransactionInput, path: Option[DerivationPath])
      extends PathInterpreter(path) {

      def value = input.getValue

    }

    class WalletTransactionOutput(val output: TransactionOutput, path: Option[DerivationPath])
    extends PathInterpreter(path) {

      def value = output.getValue

    }

    class PathInterpreter(val path: Option[DerivationPath]) {
      def isMine = path.isDefined
      def isOnExternalBranch = path.exists(_(1).get.index == 0)
      def isOnInternalBranch = {
        path.exists(_(1).get.index == 1)
      }
    }

  }
}


