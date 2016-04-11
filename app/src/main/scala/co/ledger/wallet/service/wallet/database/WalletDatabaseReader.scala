/**
 *
 * WalletDatabaseReader
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
package co.ledger.wallet.service.wallet.database

import android.database.Cursor
import android.database.sqlite.{SQLiteQueryBuilder, SQLiteDatabase}
import DatabaseStructure._
import co.ledger.wallet.service.wallet.database.cursor.{TransactionCursor, AccountCursor}
import co.ledger.wallet.service.wallet.database.model.TransactionRow

import scala.collection.mutable.ArrayBuffer

class WalletDatabaseReader(database: SQLiteDatabase) {

  import DatabaseStructure._

  def allAccounts(): AccountCursor = {
    import DatabaseStructure.AccountTableColumns._
    AccountCursor(
      database.query(
        AccountTableName,
        AccountTableColumns.projection,
        null,
        null,
        null,
        null,
        s"$Index ASC"
      )
    )
  }

  def allOperations(offset: Int = 0, limit: Int = -1): Cursor = null

  def accountOperations(accountIndex: Int, offset: Int = 0, limit: Int = -1): Cursor = {
    import DatabaseStructure.OperationTableColumns.FullOperationProjection._
    val SelectFullOperation =
      s"""
         | SELECT ${allFieldProjectionKeys.mkString(",")} FROM $OperationTableName
         | JOIN $TransactionTableName ON ${Keys.TransactionHash} = ${Keys.TransactionJoinKey}
         | JOIN $AccountTableName ON ${Keys.AccountIndex} = ${Keys.AccountJoinKey}
         | LEFT OUTER JOIN $BlockTableName ON ${Keys.BlockHash} = ${Keys.BlockJoinKey}
         | WHERE ${Keys.AccountIndex} = $accountIndex
         | ORDER BY ${Keys.TransactionTime} DESC
         | LIMIT $limit OFFSET $offset
     """.stripMargin
    SelectFullOperation.execute()
  }

  def unconfirmedAccountOperations(accountIndex: Int, offset: Int = 0, limit: Int = -1): Cursor = {
    import DatabaseStructure.OperationTableColumns.FullOperationProjection._
    var SelectFullOperation =
      s"""
         | SELECT ${allFieldProjectionKeys.mkString(",")} FROM $OperationTableName
         | JOIN $TransactionTableName ON ${Keys.TransactionHash} = ${Keys.TransactionJoinKey}
         | JOIN $AccountTableName ON ${Keys.AccountIndex} = ${Keys.AccountJoinKey}
         | LEFT OUTER JOIN $BlockTableName ON ${Keys.BlockHash} = ${Keys.BlockJoinKey}
         | WHERE ${Keys.AccountIndex} = $accountIndex AND ${Keys.BlockHash} IS NULL
         | ORDER BY ${Keys.TransactionTime} DESC
     """.stripMargin
    if (limit != -1)
      SelectFullOperation += s"LIMIT $limit OFFSET $offset"
    SelectFullOperation.execute()
  }

  def operationInputs(operationUid: String): Cursor = null

  def operationOutputs(operationUid: String): Cursor = null

  def allFullOperations(offset: Int = 0, limit: Int = -1): Cursor = {
    import DatabaseStructure.OperationTableColumns.FullOperationProjection._
    val SelectFullOperation =
      s"""
         | SELECT ${allFieldProjectionKeys.mkString(",")} FROM $OperationTableName
         | JOIN $TransactionTableName ON ${Keys.TransactionHash} = ${Keys.TransactionJoinKey}
         | JOIN $AccountTableName ON ${Keys.AccountIndex} = ${Keys.AccountJoinKey}
         | LEFT OUTER JOIN $BlockTableName ON ${Keys.BlockHash} = ${Keys.BlockJoinKey}
         | ORDER BY ${Keys.TransactionTime} DESC
         | LIMIT $limit OFFSET $offset
     """.stripMargin
    SelectFullOperation.execute()
  }

  def allPendingFullOperations(offset: Int = 0, limit: Int = -1): Cursor = {
    import DatabaseStructure.OperationTableColumns.FullOperationProjection._
    val SelectPendingFullOperation =
      s"""
         | SELECT ${allFieldProjectionKeys.mkString(",")} FROM $OperationTableName
         | JOIN $TransactionTableName ON ${Keys.TransactionHash} = ${Keys.TransactionJoinKey}
         | JOIN $AccountTableName ON ${Keys.AccountIndex} = ${Keys.AccountJoinKey}
         | LEFT OUTER JOIN $BlockTableName ON ${Keys.BlockHash} = ${Keys.BlockJoinKey}
         | WHERE ${Keys.BlockHash} IS NULL
         | ORDER BY ${Keys.TransactionTime} DESC
         | LIMIT $limit OFFSET $offset
     """.stripMargin
    SelectPendingFullOperation.execute()
  }

  def querySingleFullOperation(uid: String): Cursor = {
    import DatabaseStructure.OperationTableColumns.FullOperationProjection._
    val SelectFullOperation =
      s"""
         | SELECT ${allFieldProjectionKeys.mkString(",")} FROM $OperationTableName
         | JOIN $TransactionTableName ON ${Keys.TransactionHash} = ${Keys.TransactionJoinKey}
         | JOIN $AccountTableName ON ${Keys.AccountIndex} = ${Keys.AccountJoinKey}
         | JOIN $BlockTableName ON ${Keys.BlockHash} = ${Keys.BlockJoinKey}
         | WHERE ${Keys.Uid} = "$uid"
     """.stripMargin
    SelectFullOperation.execute()
  }

  def operationCount(): Int = {
    val SelectCountOperation =
      s"""
         | SELECT COUNT(*) FROM $OperationTableName
     """.stripMargin

    val cursor = SelectCountOperation.execute()
    cursor.moveToFirst()
    val count = cursor.getInt(0)
    cursor.close()
    count
  }

  def lastBlock(): Cursor = {
    import DatabaseStructure.BlockTableColumns._
    val SelectLastBlock =
      s"""
        | SELECT ${projection.mkString(",")} FROM $BlockTableName
        | ORDER BY $Height DESC
        | LIMIT 1
    """.stripMargin
    SelectLastBlock.execute()
  }

  def lastUsedPath(account: Int, node: Int): Option[String] = {
    import DatabaseStructure.OutputTableColumns._
    val l = account.toString.length + node.toString.length
    val SelectLastPath =
      s"""
        | SELECT $Path FROM $OutputTableName
        | WHERE $Path LIKE 'm/$account''/$node/%'
        | ORDER BY CAST(SUBSTR($Path, ${6 + l}) AS INTEGER) DESC
        | LIMIT 1
      """.stripMargin
    val cursor = SelectLastPath.execute()
    if (cursor.getCount == 1 && cursor.moveToFirst()) {
      Some(cursor.getString(0))
    } else
      None
  }

  def utxo(account: Int): Cursor = {
    import DatabaseStructure.OutputTableColumns.UtxoProjection._
    val SelectUtxo =
      s"""
        | SELECT ${projection.mkString(",")} FROM $OutputTableName
        | LEFT OUTER JOIN $InputTableName ON
        |   $InputTransactionHash = $OutputTransactionHash AND
        |   $InputIndex = $OutputIndex
        | WHERE
        |   $InputTransactionHash IS NULL AND
        |   $OutputPath IS NOT NULL AND
        |   $OutputPath LIKE 'm/$account''/%'
      """.stripMargin
    SelectUtxo.execute()
  }

  def selectTransactionHashByBlockHash(blockHash: String): Array[String] = {
    import DatabaseStructure.TransactionTableColumns._

    val SelectTransactions =
      s"""
         | SELECT $Hash FROM $TransactionTableName WHERE $BlockHash = ?
       """.stripMargin
    val cursor = SelectTransactions.execute(Array(blockHash))
    val result = ArrayBuffer[String]()
    while (cursor.moveToNext()) {
      result += cursor.getString(0)
    }
    result.toArray
  }

  def transaction(hash: String): Option[TransactionRow] = {
    import DatabaseStructure.TransactionTableColumns.TransactionWithBlockProject._
    val SelectTransaction =
      s"""
        | SELECT ${projection.mkString(",")} FROM $TransactionTableName
        | LEFT OUTER JOIN $BlockTableName ON
        |   $BlockHash = $BlockJoinKey
        | WHERE
        |   $Hash = ?
      """.stripMargin
    val cursor = new TransactionCursor(SelectTransaction.execute(Array(hash)))
    if (cursor.getCount == 0 || !cursor.moveToFirst())
      None
    else {
      val row = new TransactionRow(cursor)
      cursor.close()
      Some(row)
    }
  }

  private implicit class SqlString(val sql: String) {

    def execute(params: Array[String] = Array()): Cursor = {
      database.rawQuery(sql, params)
    }

  }

}
