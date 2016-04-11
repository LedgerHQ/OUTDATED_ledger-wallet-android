/**
 *
 * WalletDatabaseInsertHelper
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

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import co.ledger.wallet.core.utils.{BitcoinjUtils, HexUtils}
import co.ledger.wallet.service.wallet.database.DatabaseStructure._
import co.ledger.wallet.service.wallet.database.proxy.{BlockProxy, TransactionInputProxy, TransactionOutputProxy, TransactionProxy}
import co.ledger.wallet.service.wallet.database.utils.DerivationPathBag
import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams

import scala.collection.JavaConverters._
import scala.util.Try

class WalletDatabaseWriter(database: SQLiteDatabase) {

  def deleteAllAccounts(): Int = {
    database.delete(AccountTableName, "", null)
  }

  def deleteTransaction(hash: String): Int = {
    import TransactionTableColumns._
    database.delete(TransactionTableName, s"$Hash = ?", Array(hash))
  }

  def createAccountRow(index: Option[Int] = None,
                       name: Option[String] = None,
                       color: Option[Int] = None,
                       hidden: Option[Boolean] = None,
                       xpub58: Option[String] = None,
                       creationTime: Option[Long] = None)
  : Boolean = {
    import DatabaseStructure.AccountTableColumns._
    val values = new ContentValues()
    index.foreach((index) => values.put(Index, Integer.valueOf(index)))
    name.foreach(values.put(Name, _))
    color.foreach((color) => values.put(Color, Integer.valueOf(color)))
    hidden.foreach(values.put(Hidden, _))
    xpub58.foreach(values.put(Xpub58, _))
    creationTime.foreach((time) => values.put(CreationTime, java.lang.Long.valueOf(time)))
    database.insertOrThrow(AccountTableName, null, values) != -1
  }

  def updateOrCreateBlock(block: BlockProxy): Boolean = {
    import DatabaseStructure.BlockTableColumns._
    val values = new ContentValues()
    values.put(Hash, block.hash)
    values.put(Height, java.lang.Integer.valueOf(block.height))
    values.put(Time, JLong(block.time.getTime / 1000L))
    updateOrCreate(BlockTableName, values, s"$Hash = ?", Array(block.hash))
  }

  def updateOrCreateOperation(accountId: Int,
                              transactionHash: String,
                              operationType: Int,
                              value: Long,
                              senders: Array[String],
                              recipients: Array[String]): Boolean = {
    import DatabaseStructure.OperationTableColumns._
    val uid = computeOperationUid(accountId, transactionHash, operationType)
    val values = new ContentValues()
    values.put(Uid, uid)
    values.put(AccountId, java.lang.Integer.valueOf(accountId))
    values.put(TransactionHash, transactionHash)
    values.put(Type, java.lang.Integer.valueOf(operationType))
    values.put(Value, JLong(value))
    values.put(Senders, senders.mkString(","))
    values.put(Recipients, recipients.mkString(","))
    updateOrCreate(OperationTableName, values, s"$Uid = ?", Array(uid))
  }

  def computeOperationUid(accountId: Int, transactionHash: String, operationType: Int) =
    Array(transactionHash, operationType, accountId).mkString("_")

  def updateTransactionBlock(txHash: String, blockHash: String): Boolean
  = {
    import DatabaseStructure.TransactionTableColumns._
    val values = new ContentValues()
    values.put(BlockHash, blockHash)
    database.update(TransactionTableName, values, s"$Hash = ?", Array(txHash)) > 0
  }

  def updateTransactionsBlock(hashes: Array[String], blockHash: String): Boolean
  = {
    import DatabaseStructure.TransactionTableColumns._
    val values = new ContentValues()
    values.put(BlockHash, blockHash)
    val stringifiedHashes = "(" + hashes.map("\"" + _ + "\"").mkString(",") + ")"
    database.update(TransactionTableName, values, s"$Hash IN $stringifiedHashes", null) > 0
  }

  def updateOrCreateTransaction(tx: TransactionProxy, bag: DerivationPathBag): Boolean
    = {
    import DatabaseStructure.TransactionTableColumns._
    val value = new ContentValues()

    value.put(Hash, tx.hash)
    value.put(Fees, JLong(tx.fees))
    value.put(Time, JLong(tx.time.getTime / 1000L))
    value.put(LockTime, JLong(tx.lockTime))

    value.put(BlockHash, tx.block.orNull)

    val inserted = updateOrCreate(
      TransactionTableName,
      value, s"$Hash = ?",
      Array(tx.hash)
    )

    for (input <- tx.inputs) {
      updateOrCreateTransactionInput(input, Some(tx), Some(bag))
    }

    val outputs = tx.outputs
    for (index <- outputs.indices) {
      updateOrCreateTransactionOutput(outputs(index), tx, index, Some(bag))
    }

    inserted
  }

  def updateOrCreateTransactionInput(input: TransactionInputProxy,
                                     tx: Option[TransactionProxy],
                                     bag: Option[DerivationPathBag]): Boolean = {
    import DatabaseStructure.InputTableColumns._
    var uid: String = null
    val values = new ContentValues()

    if (input.isCoinbase) {
      uid = input.coinbase
      values.put(Coinbase, true)
    } else {
      uid = s"${input.previousTxHash}_${input.index}"
      values.put(Index, JLong(input.index))
      val path = bag.flatMap(_.findPath(input))
      if (path.isDefined)
        values.put(Path, path.get.toString)
      values.put(Value, JLong(input.value))
      values.put(PreviousTx, input.previousTxHash)
      values.put(ScriptSig, input.scriptSigHex)
      values.put(Address, input.address.orNull)
    }
    values.put(Uid, uid)
    val inserted = updateOrCreate(InputTableName, values, s"$Uid = ?", Array(uid))

    //Optionally add or create a link
    if (tx.isDefined) {
      import DatabaseStructure.TransactionsInputsTableColumns._
      val linkValues = new ContentValues()
      linkValues.put(TransactionHash, tx.get.hash)
      linkValues.put(InputUid, uid)
      updateOrCreate(
        TransactionsInputsTableName,
        linkValues,
        s"$TransactionHash = ? AND $InputUid = ?",
        Array(tx.get.hash, uid)
      )
    }
    inserted
  }

  def updateOrCreateTransactionOutput(output: TransactionOutputProxy,
                                      tx: TransactionProxy,
                                      index: Int,
                                      bag: Option[DerivationPathBag]):
  Boolean = {
    import DatabaseStructure.OutputTableColumns._
    val uid = s"${tx.hash}_${output.index}"
    val values = new ContentValues()

    values.put(Uid, uid)
    values.put(TransactionHash, tx.hash)
    values.put(Index, JLong(index))
    val path = bag.flatMap(_.findPath(output))
    if (path.isDefined)
      values.put(Path, path.get.toString)
    values.put(Value, JLong(output.value))
    values.put(PubKeyScript, output.pubKeyScriptHex)
    values.put(Address, output.address.orNull)
    updateOrCreate(OutputTableName, values, s"$Uid = ?", Array(uid))
  }

  def transaction[A](f: => A) = {
    beginTransaction()
    val r = Try(f)
    if (r.isSuccess)
      commitTransaction()
    else
      r.failed.get.printStackTrace()
    endTransaction()
    r
  }

  def updateOrCreate(table: String, values: ContentValues, whereClause: String, whereArgs:
    Array[String]): Boolean = {
    if (database.update(table, values, whereClause, whereArgs) <= 0) {
      val ret = database.insertOrThrow(table, null, values)
      true
    } else {
      false
    }
  }

  def beginTransaction() = database.beginTransaction()
  def commitTransaction() = database.setTransactionSuccessful()
  def endTransaction() = database.endTransaction()


  private def JLong(long: Long): java.lang.Long = {
    java.lang.Long.valueOf(long)
  }

  private def JLong(coin: Coin): java.lang.Long = {
    if (coin == null) {
      null
    } else {
      JLong(coin.getValue)
    }
  }

  private def JLong(coin: Option[Coin]): java.lang.Long = {
    JLong(coin.orNull)
  }

}
