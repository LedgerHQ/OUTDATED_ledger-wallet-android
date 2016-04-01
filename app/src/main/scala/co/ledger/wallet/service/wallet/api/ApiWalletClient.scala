/**
  *
  * ApiWallet
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 17/03/16.
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
package co.ledger.wallet.service.wallet.api

import java.io.File

import android.content.Context
import android.provider.ContactsContract.Directory
import co.ledger.wallet.core.utils.HexUtils
import co.ledger.wallet.core.utils.logs.Loggable
import co.ledger.wallet.service.wallet.AbstractDatabaseStoredWallet
import co.ledger.wallet.service.wallet.api.rest.{BlockRestClient, ApiObjects, TransactionRestClient}
import co.ledger.wallet.service.wallet.database.model.AccountRow
import co.ledger.wallet.wallet.exceptions.WalletNotSetupException
import co.ledger.wallet.wallet.{Block, DerivationPath, Account, ExtendedPublicKeyProvider}
import com.squareup.okhttp.internal.DiskLruCache
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Context => JContext, Coin, Transaction, NetworkParameters}

import scala.concurrent.Future

class ApiWalletClient(context: Context, name: String, networkParameters: NetworkParameters)
  extends AbstractDatabaseStoredWallet(context, name, networkParameters) with Loggable {

  override def account(index: Int): Future[Account] = init() map {(_) =>
    _accounts(index)
  }

  override def synchronize(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = {

    def synchronizeUntilEmptyAccount(syncToken: String, from: Int, block: ApiObjects.Block): Future[Unit]
    = {
      init().flatMap { (_) =>
        val accounts = _accounts.slice(from, _accounts.length)
        Future.sequence(accounts.map(_.synchronize(syncToken, block)).toList)
      } flatMap { (_) =>
        if (_accounts.last.keyChain.getIssuedExternalKeys != 0 ||
          _accounts.last.keyChain.getIssuedInternalKeys != 0) {
          // Create a new account
          val newAccountIndex = _accounts.length
          createAccount(_accounts.length, publicKeyProvider).flatMap { (_) =>
            _accounts = Array[ApiAccountClient]()
            synchronizeUntilEmptyAccount(syncToken, newAccountIndex, block)
          }
        } else {
          Future.successful()
        }
      }
    }
    transactionRestClient.requestSyncToken().flatMap {(token) =>
      blockRestClient.mostRecentBlock().flatMap {(block) =>
        synchronizeUntilEmptyAccount(token, 0, block).flatMap { unit =>
          transactionRestClient.deleteSyncToken(token)
        }
      }
    }
  }

  override def accounts(): Future[Array[Account]] = init() map {(_) => _accounts
    .asInstanceOf[Array[Account]]
  }

  override def setup(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] =
    createAccount(0, publicKeyProvider)


  private def createAccount(index: Int, publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = {
    publicKeyProvider.generateXpub(DerivationPath(s"44'/0'/$index'")) map {(xpub) =>
      database.writer.createAccountRow(
        index = Some(index),
        hidden = Some(false),
        xpub58 = Some(xpub.serializePubB58(networkParameters)),
        creationTime = Some(0)
      )
    }
  }

  override def mostRecentBlock(): Future[Block] = {
    init().flatMap((_) => super.mostRecentBlock())
  }

  override def isSynchronizing(): Future[Boolean] = Future {false}

  override def balance(): Future[Coin] = {
    init() flatMap {unit =>
      Future.sequence((for (a <- _accounts) yield a.balance()).toSeq).map { (balances) =>
        balances.reduce(_ add _)
      }
    }
  }

  override def accountsCount(): Future[Int] = init() map {(_) => _accounts.length}

  override val eventBus: EventBus = new EventBus()

  override def pushTransaction(transaction: Transaction): Future[Unit] =
    init() flatMap {unit =>
    transactionRestClient.pushTransaction(HexUtils.bytesToHex(transaction.bitcoinSerialize()))
  }

  override def needsSetup(): Future[Boolean] = init() map {(_) =>
    false
  } recover {
    case ex: WalletNotSetupException => true
    case other: Throwable => throw other
  }

  def databaseWriter = database.writer

  private def init(): Future[Unit] = Future {
    if (_accounts.isEmpty) {
      JContext.propagate(JContext.getOrCreate(networkParameters))
      val accounts = AccountRow(database.reader.allAccounts())
      if (accounts.isEmpty)
        throw WalletNotSetupException()
      _accounts = accounts.map(new ApiAccountClient(this, _))
    }
  }

  val directory = context.getDir(s"api_$name", Context.MODE_PRIVATE)

  private var _accounts = Array[ApiAccountClient]()

  // Rest clients
  val transactionRestClient = new TransactionRestClient(context, networkParameters)
  val blockRestClient = new BlockRestClient(context, networkParameters)

  //
  // Raw tx management
  //

  def rawTransaction(hash: String): Future[Transaction] = Future {
    Option(_rawTxCache.get(hash))
  } flatMap {
    case Some(entry) =>
      Future.successful(new Transaction(networkParameters, HexUtils.decodeHex(entry.getString(0))))
    case None =>
      transactionRestClient.rawTransaction(hash).map {(hexTx) =>
        val editor = _rawTxCache.edit(hash)
        editor.set(0, hexTx)
        editor.commit()
        new Transaction(networkParameters, HexUtils.decodeHex(hexTx))
      }
  }

  private val RawTxCacheMaxSize = 24 * 1024
  private val _rawTxCache = DiskLruCache.open(new File(directory, "raw_txs_cache"), 0,
    1, RawTxCacheMaxSize)
  //
  // \Raw tx management
  //
}
