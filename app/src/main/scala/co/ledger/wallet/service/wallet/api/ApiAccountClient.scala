/**
  *
  * ApiAccount
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

import java.io.{BufferedInputStream, FileInputStream, FileReader, File}

import co.ledger.wallet.core.utils.Preferences
import co.ledger.wallet.core.utils.io.IOUtils
import co.ledger.wallet.core.utils.logs.{Logger, Loggable}
import co.ledger.wallet.service.wallet.AbstractDatabaseStoredAccount
import co.ledger.wallet.service.wallet.api.rest.ApiObjects
import co.ledger.wallet.service.wallet.database.model.{OperationRow, TransactionRow, AccountRow}
import co.ledger.wallet.wallet.{DerivationPath, ExtendedPublicKeyProvider}
import co.ledger.wallet.wallet.api.ApiWalletClientProtos
import com.google.protobuf.nano.{CodedOutputByteBufferNano, CodedInputByteBufferNano}
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.wallet.{Protos, DeterministicKeyChain}
import scala.collection.JavaConversions._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ApiAccountClient(val wallet: ApiWalletClient, row: AccountRow)
  extends AbstractDatabaseStoredAccount(wallet) with Loggable {

  private val BatchSize = 40

  override def keyChain: DeterministicKeyChain = _keychain

  override def rawTransaction(hash: String): Future[Transaction] = ???

  //
  // Synchronization methods
  //

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] = wallet
    .synchronize(provider)

  def synchronize(syncToken: String, block: ApiObjects.Block): Future[Unit] = {
    // Load previously saved state
    // Fetch all unconfirmed transactions
    // Synchronize all previous batches
      // If batches.length == 0 or last batches is not empty synchronize another batch
        // If batches not empty redo the last step
    // Remove not found unconfirmed transactions
    // Everything went ok. Save the state and normalize batches block with the block in parameter
    // Save the state

    load() flatMap {(savedState) =>
      var unconfirmedTransaction = OperationRow(databaseWallet.databaseReader
        .unconfirmedAccountOperations(index)).map(_.transactionHash)
      def synchronizeBatchUntilEmpty(from: Int = 0): Future[Unit] = {
        synchronizeBatches(syncToken, savedState, 0, savedState.batches.length - 1).flatMap {
          (fetchedTxs) =>
          unconfirmedTransaction = unconfirmedTransaction.filter(!fetchedTxs.contains(_))
          if (savedState.batches.isEmpty || savedState.batches.last.blockHash == null) {
            synchronizeBatchUntilEmpty(savedState.batches.length + 1)
          } else {
            Future.successful()
          }
        }
      }
      synchronizeBatchUntilEmpty().flatMap(_ => save(savedState))
    } map {(_) =>
      ()
    }
  }

  def synchronizeBatches(syncToken: String,
                         savedState: ApiWalletClientProtos.ApiAccountClient,
                         from: Int, to: Int):
  Future[Array[String]] = {

    def synchronizeBatch(batchIndex: Int): Future[Array[String]] = {
      val fromAddress = batchIndex * BatchSize
      val toAddress = fromAddress + (BatchSize - 1)
      def synchronizeBatchUntilEmpty(results: Array[String]): Future[Array[String]] = {
        val batch = savedState.batches(index)
        val addresses = new ArrayBuffer[String]()
        for (addressIndex <- fromAddress to toAddress) {
          import DerivationPath.dsl._
          val externalPath = 0.h / 0 / addressIndex
          val internalPath = 0.h / 1 / addressIndex
          addresses += keyChain.getKeyByPath(externalPath.toBitcoinJList, true)
            .toAddress(wallet.networkParameters).toString
          addresses += keyChain.getKeyByPath(internalPath.toBitcoinJList, true)
            .toAddress(wallet.networkParameters).toString
        }

        wallet.transactionRestClient.transactions(syncToken, addresses.toArray, Option(batch
          .blockHash)) map {(result) =>
          result.isTruncated
          Array.empty[String]
        }
      }
      while (batchIndex >= savedState.batches.length) {
        val newBatch = new ApiWalletClientProtos.ApiAccountClient.Batch()
        newBatch.index = savedState.batches.length
        savedState.batches = savedState.batches :+ newBatch
      }
      synchronizeBatchUntilEmpty(Array[String]())
    }

    Future.sequence((from to Math.max(from, to)).map(synchronizeBatch)).map {(result) =>
      result.flatten.toArray
    }
  }

  private def pushTransaction(transactions: Array[ApiObjects.Transaction],
                              hashes: ArrayBuffer[String]): Unit = {

  }

  //
  // \Synchronization methods
  //

  override def xpub(): Future[DeterministicKey] = Future.successful(_xpub)

  override def index: Int = row.index

  def load(): Future[ApiWalletClientProtos.ApiAccountClient] = Future {
    if (savedStateFile.exists()) {
      val input = IOUtils.copy(savedStateFile)
      ApiWalletClientProtos.ApiAccountClient.parseFrom(input)
    } else {
      val state = new ApiWalletClientProtos.ApiAccountClient()
      state.index = index
      state.batches = ApiWalletClientProtos.ApiAccountClient.Batch.emptyArray()
      state.batchSize = BatchSize
      state
    }
  }

  def save(savedState: ApiWalletClientProtos.ApiAccountClient): Future[Unit] = Future {
    val raw = new Array[Byte](savedState.getSerializedSize)
    val output = CodedOutputByteBufferNano.newInstance(raw)
    val tmpFile = new File(directory, "tmp_saved_state")
    IOUtils.copy(raw, tmpFile)
    tmpFile.renameTo(savedStateFile)
  }

  private val _preferences = Preferences(s"ApiAccountClient_$index")(wallet.context)
  private val _xpub = {
    val accountKey = DeterministicKey.deserializeB58(row.xpub58, wallet.networkParameters)
    new DeterministicKey(
      DeterministicKeyChain.ACCOUNT_ZERO_PATH,
      accountKey.getChainCode,
      accountKey.getPubKeyPoint,
      null, null)
  }

  val directory = new File(wallet.directory, s"account_$index/")
  directory.mkdirs()

  private val _keychain = {
    val keychainFile = new File(directory, "keychain")
    if (!keychainFile.exists()) {
      DeterministicKeyChain.watch(_xpub)
    } else {
      try {
        val reader = new BufferedInputStream(new FileInputStream(keychainFile))
        var size = 0
        val keys = new ArrayBuffer[Protos.Key]()
        def readSize(): Int = {
          val b1 = reader.read()
          val b2 = reader.read()
          if (b1 == -1 || b2 == -1) -1 else b1 << 8 | b2
        }
        while ({size = readSize(); size} != -1) {
          val bytes = new Array[Byte](size)
          reader.read(bytes, 0, bytes.length)
          keys += Protos.Key.parseFrom(bytes)
        }
        DeterministicKeyChain.fromProtobuf(keys.toList, null, null).get(0)
      } catch {
        case anything: Throwable =>
          anything.printStackTrace()
          DeterministicKeyChain.watch(_xpub)
      }
    }
  }

  private val savedStateFile = new File(directory, "saved_state")

}
