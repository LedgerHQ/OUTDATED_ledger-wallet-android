/**
  *
  * AbstractDatabaseStoreWallet
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

import java.io.File

import android.content.Context
import co.ledger.wallet.app.wallet.WalletPreferences
import co.ledger.wallet.core.concurrent.{AbstractAsyncCursor, AsyncCursor, SerialQueueTask}
import co.ledger.wallet.core.utils.logs.Loggable
import co.ledger.wallet.service.wallet.database.WalletDatabaseOpenHelper
import co.ledger.wallet.service.wallet.database.cursor.{BlockCursor, OperationCursor}
import co.ledger.wallet.service.wallet.database.model.BlockRow
import co.ledger.wallet.wallet.{Block, Operation, Wallet}
import org.bitcoinj.core.NetworkParameters

import scala.concurrent.Future

abstract class AbstractDatabaseStoredWallet(val context: Context,
                                            val name: String,
                                            val networkParameters: NetworkParameters)
  extends Wallet with SerialQueueTask with Loggable {

  override def operations(limit: Int, batchSize: Int): Future[AsyncCursor[Operation]] = Future {
    new AbstractAsyncCursor[Operation](ec, batchSize) {
      override protected def performQuery(from: Int, to: Int): Array[Operation] = {
        OperationCursor.toArray(_database.reader.allFullOperations(from, to - from))
          .asInstanceOf[Array[Operation]]
      }

      override def count: Int = {
        if (limit == -1)
          _database.reader.operationCount()
        else
          Math.min(limit, _database.reader.operationCount())
      }

      override def requery(): Future[AsyncCursor[Operation]] = operations(batchSize)
    }
  }

  def querySingleOperation(accountId: Int, transactionHash: String, opType: Int): AsyncCursor[Operation] =
    querySingleOperation(_database.writer.computeOperationUid(accountId, transactionHash, opType))

  protected def querySingleOperation(opUid: String): AsyncCursor[Operation] = {
    new AbstractAsyncCursor[Operation](ec, 1) {
      override protected def performQuery(from: Int, to: Int): Array[Operation] = {
        OperationCursor.toArray(_database.reader.querySingleFullOperation(opUid))
          .asInstanceOf[Array[Operation]]
      }

      override def count: Int = 1

      override def requery(): Future[AsyncCursor[Operation]] = Future {querySingleOperation(opUid)}
    }
  }

  override def mostRecentBlock(): Future[Block] = {
    Future {
      val cursor = new BlockCursor(database.reader.lastBlock())
      if (cursor.moveToFirst() && cursor.getCount > 0)
        new BlockRow(cursor)
      else
        throw new NoSuchElementException
    }
  }

  override def stop(): Unit = {
    database.close()
  }

  def databaseReader = _database.reader
  protected def database = _database

  private[this] lazy val _database = new WalletDatabaseOpenHelper(context, name)

}
