/**
  *
  * AbstractDatabaseStoredAccount
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
package co.ledger.wallet.service.wallet

import co.ledger.wallet.core.concurrent.{AbstractAsyncCursor, AsyncCursor}
import co.ledger.wallet.service.wallet.database.cursor.{OperationCursor, BlockCursor, OutputCursor}
import co.ledger.wallet.service.wallet.database.model.{OutputRow, BlockRow}
import co.ledger.wallet.service.wallet.spv.SpvWalletClient
import co.ledger.wallet.wallet.{Utxo, Operation, DerivationPath, Account}
import org.bitcoinj.core.{Transaction, Sha256Hash, Coin, Address}
import org.bitcoinj.crypto.ChildNumber
import org.bitcoinj.wallet.{DeterministicKeyChain, KeyChain}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future
import scala.util.Try

abstract class AbstractDatabaseStoredAccount(val databaseWallet: AbstractDatabaseStoredWallet)
  extends Account {

  implicit val ec = databaseWallet.ec
  def keyChain: DeterministicKeyChain

  override def freshPublicAddress(): Future[Address] = Future {
    val reader = wallet.asInstanceOf[SpvWalletClient].databaseReader
    val path = DerivationPath(reader.lastUsedPath(index, 0).getOrElse(s"m/$index'/0/0"))
    val newPath = new DerivationPath(path.parent, path.childNum + 1)
    val childNums = newPath.toBitcoinJList
    childNums.set(0, new ChildNumber(0, true))
    val key = keyChain.getKeyByPath(childNums, true)
    key.toAddress(databaseWallet.networkParameters)
  }

  override def freshChangeAddress(): Future[(Address, DerivationPath)] = Future {
    import DerivationPath.dsl._
    val reader = wallet.asInstanceOf[SpvWalletClient].databaseReader
    val path = DerivationPath(reader.lastUsedPath(index, 1).getOrElse(s"m/$index'/1/0"))
    val newPath = new DerivationPath(path.parent, path.childNum + 1)
    val childNums = newPath.toBitcoinJList
    childNums.set(0, new ChildNumber(0, true))
    val key = keyChain.getKeyByPath(childNums, true)
    val bip44Path = 44.h/0.h ++ newPath
    key.toAddress(databaseWallet.networkParameters) -> bip44Path
  }

  override def operations(limit: Int, batchSize: Int): Future[AsyncCursor[Operation]] = Future {
    new AbstractAsyncCursor[Operation](ec, batchSize) {
      override protected def performQuery(from: Int, to: Int): Array[Operation] = {
        OperationCursor.toArray(databaseWallet.databaseReader.accountOperations(index, from, to - from))
          .asInstanceOf[Array[Operation]]
      }

      override def count: Int = {
        if (limit == -1)
          databaseWallet.databaseReader.operationCount()
        else
          Math.min(limit, databaseWallet.databaseReader.operationCount())
      }

      override def requery(): Future[AsyncCursor[Operation]] = operations(batchSize)
    }
  }

  override def balance(): Future[Coin] = utxoRows().map(_.map(_.value).foldLeft(Coin.ZERO)(_ add _))

  override def utxo(targetValue: Option[Coin]): Future[Array[Utxo]] =
    utxoRows(targetValue) map {(outputs) =>
      val database = databaseWallet.databaseReader
      val lastBlockCursor = new BlockCursor(database.lastBlock())
      lastBlockCursor.moveToFirst()
      val lastBlock = new BlockRow(lastBlockCursor)
      lastBlockCursor.close()
      val result = new ArrayBuffer[Utxo]()
      outputs.foreach {(output) =>
        database.transaction(output.transactionHash) match {
          case Some(tx) =>
            val childNums = output.path.get.toBitcoinJList
            childNums.set(0, new ChildNumber(0, true))
            val publicKey = keyChain.getKeyByPath(childNums,
              false).getPubKey
            result += Utxo(rawTransaction(output.transactionHash), output, lastBlock.height - tx
              .blockHeight.map(_
              .toInt).getOrElse
            (lastBlock
              .height), publicKey)
          case None => // Do nothing
        }
      }
      result.toArray
  }

  private def utxoRows(targetValue: Option[Coin] = None): Future[Array[OutputRow]] = Future {
    val database = databaseWallet.databaseReader
    val cursor = new OutputCursor(database.utxo(index))
    var collectedValue = Coin.ZERO
    if (cursor.getCount == 0 || !cursor.moveToFirst()) {
      cursor.close()
      Array.empty[OutputRow]
    } else {
      val result = new ArrayBuffer[OutputRow]()
      do {
        val row = new OutputRow(cursor)
        collectedValue = collectedValue add row.value
        result += row
      } while (cursor.moveToNext() && (targetValue.isEmpty || targetValue.get.isLessThan(collectedValue)))
      cursor.close()
      result.toArray
    }
  }

  def rawTransaction(hash: String): Future[Transaction]

}
