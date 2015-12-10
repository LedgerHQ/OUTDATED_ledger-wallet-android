/**
 *
 * AccountCursor
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

import android.database.Cursor
import co.ledger.wallet.service.wallet.database.DatabaseStructure.AccountTableColumns.ProjectionIndex._

case class AccountCursor(override val self: Cursor) extends CursorExtension(self) {

  def index = self.getInt(Index)
  def name = self.getString(Name)
  def color = self.getInt(Color)
  def isHidden = self.getInt(Hidden) != 0
  def xpub58 = self.getString(Xpub58)
  def creationTime = self.getLong(CreationTime)

}
