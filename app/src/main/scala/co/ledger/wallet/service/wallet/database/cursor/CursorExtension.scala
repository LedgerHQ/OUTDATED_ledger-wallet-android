/**
 *
 * CursorExtension
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
package co.ledger.wallet.service.wallet.database.cursor

import android.content.ContentResolver
import android.database.{ContentObserver, CharArrayBuffer, DataSetObserver, Cursor}
import android.net.Uri
import android.os.Bundle

class CursorExtension(val self: Cursor) extends Cursor {
  override def moveToFirst(): Boolean = self.moveToFirst()

  override def getType(columnIndex: Int): Int = self.getType(columnIndex)

  override def isBeforeFirst: Boolean = self.isBeforeFirst

  override def getPosition: Int = self.getPosition

  override def move(offset: Int): Boolean = self.move(offset)

  override def getNotificationUri: Uri = self.getNotificationUri

  override def registerContentObserver(observer: ContentObserver): Unit = self
    .registerContentObserver(observer)

  override def getExtras: Bundle = self.getExtras

  override def moveToNext(): Boolean = self.moveToNext()

  override def isAfterLast: Boolean = self.isAfterLast

  override def getWantsAllOnMoveCalls: Boolean = self.getWantsAllOnMoveCalls

  override def getColumnIndex(columnName: String): Int = self.getColumnIndex(columnName)

  override def moveToPrevious(): Boolean = self.moveToPrevious()

  override def isLast: Boolean = self.isLast

  override def getDouble(columnIndex: Int): Double = self.getDouble(columnIndex)

  override def unregisterContentObserver(observer: ContentObserver): Unit = self
    .unregisterContentObserver(observer)

  override def isFirst: Boolean = self.isFirst

  override def getColumnIndexOrThrow(columnName: String): Int = self.getColumnIndexOrThrow(columnName)

  override def getCount: Int = self.getCount

  override def moveToLast(): Boolean = self.moveToLast()

  override def getColumnCount: Int = self.getColumnCount

  override def getColumnName(columnIndex: Int): String = self.getColumnName(columnIndex)

  override def getFloat(columnIndex: Int): Float = self.getFloat(columnIndex)

  override def registerDataSetObserver(observer: DataSetObserver): Unit = self
    .registerDataSetObserver(observer)

  override def getLong(columnIndex: Int): Long = self.getLong(columnIndex)

  override def deactivate(): Unit = self.deactivate()

  override def copyStringToBuffer(columnIndex: Int, buffer: CharArrayBuffer): Unit = self
    .copyStringToBuffer(columnIndex, buffer)

  override def requery(): Boolean = self.requery()

  override def moveToPosition(position: Int): Boolean = self.moveToPosition(position)

  override def setNotificationUri(cr: ContentResolver, uri: Uri): Unit = self.setNotificationUri(cr, uri)

  override def unregisterDataSetObserver(observer: DataSetObserver): Unit = self
    .unregisterDataSetObserver(observer)

  override def getShort(columnIndex: Int): Short = self.getShort(columnIndex)

  override def isNull(columnIndex: Int): Boolean = self.isNull(columnIndex)

  override def respond(extras: Bundle): Bundle = self.respond(extras)

  override def close(): Unit = self.close()

  override def setExtras(extras: Bundle): Unit = self.setExtras(extras)

  override def isClosed: Boolean = self.isClosed

  override def getColumnNames: Array[String] = self.getColumnNames

  override def getInt(columnIndex: Int): Int = self.getInt(columnIndex)

  override def getBlob(columnIndex: Int): Array[Byte] = self.getBlob(columnIndex)

  override def getString(columnIndex: Int): String = self.getString(columnIndex)
}
