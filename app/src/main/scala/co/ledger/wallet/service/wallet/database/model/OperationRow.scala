/**
 *
 * OperationRow
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
package co.ledger.wallet.service.wallet.database.model

import java.util.Date

import co.ledger.wallet.service.wallet.database.cursor.OperationCursor
import co.ledger.wallet.wallet.Operation
import org.bitcoinj.core.{Sha256Hash, Address, Coin}

class OperationRow(cursor: OperationCursor) extends Operation {
  override val uid = cursor.uid
  override val accountIndex = cursor.accountIndex
  override val transactionHash = cursor.transactionHash
  override val operationType = cursor.operationType
  override val value = Coin.valueOf(cursor.value)
  override val senders = cursor.senders.split(",")
  override val recipients = cursor.recipients.split(",")
  override val accountName = cursor.accountName
  override val accountColor = cursor.accountColor
  override val fees = Coin.valueOf(cursor.fees)
  override val time = new Date(cursor.time)
  override val lockTime = cursor.lockTime
  override val blockHash = cursor.blockHash
  override val blockHeight = cursor.blockHeight

}
