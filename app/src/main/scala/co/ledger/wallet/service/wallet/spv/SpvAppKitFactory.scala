/**
 *
 * SpvSynchronizationHelper
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

import java.io.{IOException, InputStream, File}
import java.util.Date
import java.util.concurrent.TimeUnit

import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.service.wallet.database.WalletDatabaseOpenHelper
import co.ledger.wallet.service.wallet.database.model.AccountRow
import org.bitcoinj.core.{Wallet => JWallet, _}
import org.bitcoinj.crypto.{DeterministicHierarchy, LazyECPoint, DeterministicKey}
import org.bitcoinj.net.discovery.{PeerDiscovery, DnsDiscovery}
import org.bitcoinj.store.SPVBlockStore
import org.bitcoinj.wallet.{DeterministicKeyChain, KeyChainGroup}
import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Promise, Future}

class SpvAppKitFactory(executionContext: ExecutionContext,
                       networkParameters: NetworkParameters,
                       directory: File,
                       database: WalletDatabaseOpenHelper) extends Loggable {

  implicit val DisableLogging = false
  implicit val ec = executionContext

  def setup(wallets: Array[DeterministicKey], fastCatchupDate: Date, checkpoints: InputStream): Future[SpvAppKit] = Future {
    Context.propagate(Context.getOrCreate(networkParameters))
    if (!directory.exists() && !directory.mkdirs())
      throw new IOException(s"Unable to create directory ${directory.getPath}")

    // Cleanup files
    if (chainFile.exists()) chainFile.delete()

    // Create SPV modules
    val blockStore = new SPVBlockStore(networkParameters, chainFile)
    val time = fastCatchupDate.getTime / 1000L

    if (checkpoints != null) {
      CheckpointManager.checkpoint(networkParameters, checkpoints, blockStore, time)
    }

    val blockChain = new BlockChain(networkParameters, blockStore)
    val peerGroup = new PeerGroup(networkParameters, blockChain)

    val accounts = createAccountsFromXpub(wallets, time)
    var builder = new SpvAppKitBuilder()
      .withBlockStore(blockStore)
      .withBlockChain(blockChain)
      .withPeerGroup(peerGroup)
    accounts.foreach {
      case (row, wallet) => builder = builder.addAccount(row, wallet)
    }
    builder.build()
  }

  def loadFromDatabase(): Future[SpvAppKit] = Future {
    Context.propagate(Context.getOrCreate(networkParameters))
    val accounts = allAccounts()
    Logger.d(s"Accounts count == ${accounts.length}")
    if (accounts.length == 0 || !chainFile.exists()) throw NoAppKitToLoadException()
    val blockStore = new SPVBlockStore(networkParameters, chainFile)
    val blockChain = new BlockChain(networkParameters, blockStore)
    val peerGroup = new PeerGroup(networkParameters, blockChain)

    if (blockStore.getChainHead.getHeight == 0)
      throw CorruptedBlockStoreException()

    var builder = new SpvAppKitBuilder()
      .withBlockStore(blockStore)
      .withBlockChain(blockChain)
      .withPeerGroup(peerGroup)
    accounts.foreach {
      case (row, wallet) => builder = builder.addAccount(row, wallet)
    }
    builder.build()
  }


  private def allAccounts() = {
    val cursor = database.reader.allAccounts()
    val wallets = new Array[(AccountRow, JWallet)](cursor.getCount)
    Logger.d(s"Number of accounts ${cursor.getCount}")
    if (cursor.getCount > 0) {
      cursor.moveToFirst()
      var index = 0
      while (!cursor.isAfterLast) {
        val row = new AccountRow(cursor)
        val xpub = xpub58ToRootAccountKey(row.xpub58)
        val walletFile =
          new File(directory, s"${networkParameters.getId}_${xpub.serializePubB58(networkParameters)}.wallet")
        val wallet: JWallet = JWallet.fromWatchingKey(networkParameters, xpub, row.creationTime)
        if (walletFile.exists()) {
          val saved = JWallet.loadFromFile(walletFile)
          // TODO: Report bug to bitcoinj + use a wallet factory to prevent recreating wallet
          // each time
          for (tx <- saved.getWalletTransactions.asScala)
            wallet.addWalletTransaction(tx)
        }
        wallet.autosaveToFile(walletFile, 1L, TimeUnit.SECONDS, null)
        wallets(index) = (row, wallet)
        index += 1
        cursor.moveToNext()
      }
    }
    wallets
  }

  private def xpub58ToRootAccountKey(xpub58: String): DeterministicKey = {
    val accountKey = DeterministicKey.deserializeB58(xpub58, networkParameters)
    new DeterministicKey(
      DeterministicKeyChain.ACCOUNT_ZERO_PATH,
      accountKey.getChainCode,
      accountKey.getPubKeyPoint,
      null, null)
  }

  def discovery = _discovery
  def discovery_=(discovery: PeerDiscovery): Unit = {
    require(discovery != null)
    _discovery = discovery
  }

  private def createAccountsFromXpub(xpubs: Array[DeterministicKey],
                                     time: Long,
                                     startIndex: Int = 0): Array[(AccountRow, JWallet)] = {
    database.writer transaction {
      for (index <- xpubs.indices) {
        val xpub = xpubs(index)
        database.writer.createAccountRow(
          index = Some(startIndex + index),
          xpub58 = Some(xpub.serializePubB58(networkParameters)),
          creationTime = Some(time)
        )
      }
    }
    allAccounts()
  }

  private[this] lazy val chainFile = new File(directory, "chain")
  private[this] var _discovery: PeerDiscovery = new DnsDiscovery(networkParameters)

}

case class NoAppKitToLoadException() extends Exception
case class CorruptedBlockStoreException() extends Exception

