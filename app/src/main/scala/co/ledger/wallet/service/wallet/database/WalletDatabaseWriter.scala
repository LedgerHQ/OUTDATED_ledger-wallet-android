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
import DatabaseStructure._
import co.ledger.wallet.service.wallet.database.model.AccountRow

import scala.util.Try

class WalletDatabaseWriter(database: SQLiteDatabase) {

  def deleteAllAccounts(): Int = {
    import DatabaseStructure.AccountTableColumns._
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

  def transaction[A](f: => A) = {
    beginTransaction()
    val r = Try(f)
    if (r.isSuccess)
      commitTransaction()
    endTransaction()
    r
  }

  def beginTransaction() = database.beginTransaction()
  def commitTransaction() = database.setTransactionSuccessful()
  def endTransaction() = database.endTransaction()

}
