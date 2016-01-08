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
import co.ledger.wallet.core.crypto.Crypto
import co.ledger.wallet.core.utils.HexUtils
import co.ledger.wallet.service.wallet.database.DatabaseStructure._
import co.ledger.wallet.service.wallet.database.utils.DerivationPathBag
import org.bitcoinj.core._
import org.bitcoinj.crypto.HDUtils

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

class WalletDatabaseWriter(database: SQLiteDatabase) {

  def deleteAllAccounts(): Int = {
    database.delete(AccountTableName, "1", null)
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

  def updateOrCreateOperation(accountId: Int,
                              transactionHash: String,
                              operationType: Int,
                              value: Long): Boolean = {
    import DatabaseStructure.OperationTableColumns._
    val uid = Array(transactionHash, operationType, accountId).mkString("_")
    val values = new ContentValues()
    values.put(Uid, uid)
    values.put(AccountId, java.lang.Integer.valueOf(accountId))
    values.put(TransactionHash, transactionHash)
    values.put(Type, java.lang.Integer.valueOf(operationType))
    values.put(Value, JLong(value))
    updateOrCreate(OperationTableName, values, s"$Uid = ?", Array(uid))
  }

  def updateOrCreateTransaction(tx: Transaction, blockChain: BlockChain, bag: DerivationPathBag):
  Boolean
    = {
    import DatabaseStructure.TransactionTableColumns._
    val value = new ContentValues()

    value.put(Hash, tx.getHashAsString)
    value.put(Fees, JLong(tx.getFee))
    value.put(Time, JLong(tx.getUpdateTime.getTime / 1000L))
    value.put(LockTime, JLong(tx.getLockTime))

    val block: Option[(Sha256Hash, Integer)] = tx.getAppearsInHashes.asScala.toList.sortBy(_._2)
      .headOption.flatMap({(blockData) =>
        Option(blockChain.getBlockStore.get(blockData._1)).map[(Sha256Hash, Integer)]({(block) =>
          (blockData._1, block.getHeight)
        })
    })

    value.put(BlockHash, block.map(_._1.toString).orNull)
    value.put(BlockHeight, block.map(_._2).orNull)

    val inserted = updateOrCreate(
      TransactionTableName,
      value, s"$Hash = ?",
      Array(tx.getHashAsString)
    )

    for (input <- tx.getInputs.asScala) {
      updateOrCreateTransactionInput(input, Some(tx), Some(bag))
    }

    val outputs = tx.getOutputs
    for (index <- 0 until outputs.size()) {
      updateOrCreateTransactionOutput(outputs.get(index), tx, index, Some(bag))
    }

    inserted
  }

  def updateOrCreateTransactionInput(input: TransactionInput,
                                     tx: Option[Transaction],
                                     bag: Option[DerivationPathBag]): Boolean = {
    import DatabaseStructure.InputTableColumns._
    var uid: String = null
    val values = new ContentValues()

    if (input.isCoinBase) {
      uid = HexUtils.bytesToHex(input.getScriptBytes)
      values.put(Coinbase, true)
    } else {
      uid = s"${input.getOutpoint.getHash.toString}_${input.getOutpoint.getIndex}"
      values.put(Index, JLong(input.getOutpoint.getIndex))
      val path = bag.flatMap(_.findKey(input)).map(_.getPath)
      if (path.isDefined)
        values.put(Path, HDUtils.formatPath(path.get))
      values.put(Value, JLong(input.getValue))
      values.put(PreviousTx, input.getOutpoint.getHash.toString)
      values.put(ScriptSig, HexUtils.bytesToHex(input.getScriptBytes))
      val address = Try(input.getConnectedOutput.getScriptPubKey.getToAddress(input.getParams, true))
        .recoverWith({
          case all => Try(new Address(tx.get.getParams, Utils.sha256hash160(input.getScriptSig.getPubKey)))
        })
        .toOption
      values.put(Address, address.map(_.toString).orNull)
    }
    values.put(Uid, uid)
    val inserted = updateOrCreate(InputTableName, values, s"$Uid = ?", Array(uid))

    //Optionally add or create a link
    if (tx.isDefined) {
      import DatabaseStructure.TransactionsInputsTableColumns._
      val linkValues = new ContentValues()
      linkValues.put(TransactionHash, tx.get.getHashAsString)
      linkValues.put(InputUid, uid)
      updateOrCreate(
        TransactionsInputsTableName,
        linkValues,
        s"$TransactionHash = ? AND $InputUid = ?",
        Array(tx.get.getHashAsString, uid)
      )
    }
    inserted
  }

  def updateOrCreateTransactionOutput(output: TransactionOutput,
                                      tx: Transaction,
                                      index: Int,
                                      bag: Option[DerivationPathBag]):
  Boolean = {
    import DatabaseStructure.OutputTableColumns._
    val uid = s"${tx.getHashAsString}_${output.getIndex}"
    val values = new ContentValues()

    values.put(Uid, uid)
    values.put(TransactionHash, tx.getHashAsString)
    values.put(Index, JLong(index))
    val path = bag.flatMap(_.findKey(output)).map(_.getPath)
    if (path.isDefined)
      values.put(Path, HDUtils.formatPath(path.get))
    values.put(Value, JLong(output.getValue))
    values.put(PubKeyScript, HexUtils.bytesToHex(output.getScriptBytes))
    values.put(Address, Try(output.getScriptPubKey.getToAddress(tx.getParams).toString).getOrElse(null))

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
    Log.d("Toto", s"Try to update $table ${whereArgs.head}")
    if (database.update(table, values, whereClause, whereArgs) <= 0) {
      Log.d("Toto", s"Failed to update $table ${whereArgs.head}, insert instead")
      val ret = database.insertOrThrow(table, null, values)
      Log.d("Toto", s"Insertion of $table ${whereArgs.head} ended with $ret")
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

}
