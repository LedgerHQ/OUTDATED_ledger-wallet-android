/**
 *
 * OperationCursor
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/01/16.
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
package co.ledger.wallet.service.wallet.database.cursor

import android.database.Cursor
import co.ledger.wallet.service.wallet.database.DatabaseStructure.OperationTableColumns.FullOperationProjection.AllFieldsProjectionIndex._
import co.ledger.wallet.service.wallet.database.model.OperationRow

import scala.collection.mutable.ArrayBuffer

case class OperationCursor(override val self: Cursor) extends CursorExtension(self) {

  def uid = self.getString(Uid)
  def accountIndex = self.getInt(AccountIndex)
  def transactionHash = self.getString(TransactionHash)
  def operationType = self.getInt(Type)
  def value = self.getLong(Value)
  def senders = self.getString(Senders)
  def recipients = self.getString(Recipients)
  def accountName = self.getString(AccountName)
  def accountColor = self.getInt(AccountColor)
  def fees = self.getLong(TransactionFees)
  def time = self.getLong(TransactionTime)
  def lockTime = self.getLong(TransactionLockTime)
  def blockHash = self.getString(BlockHash)
  def blockHeight = self.getLong(BlockHeight)
}

object OperationCursor {

  def toArray(cursor: Cursor): Array[OperationRow] = {
    val c = OperationCursor(cursor)
    val ops = new ArrayBuffer[OperationRow](c.getCount)
    if (c.moveToFirst()) {
      do {
        ops += new OperationRow(c)
      } while (c.moveToNext())
    }
    c.close()
    ops.toArray
  }

}