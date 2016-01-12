/**
 *
 * DatabaseStructure
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

import android.provider.BaseColumns

object DatabaseStructure {

  val AccountTableName = "account"
  val BlockTableName = "block"
  val TransactionTableName = "tx"
  val OperationTableName = "operation"
  val InputTableName = "input"
  val OutputTableName = "output"
  val TransactionsInputsTableName = "transactions_inputs"

  object AccountTableColumns extends BaseColumns {
    val Index = "account_index"
    val Name = "name"
    val Color = "color"
    val Hidden = "hidden"
    val Xpub58 = "xpub58"
    val CreationTime = "creation_time"

    val projection = Array(Index, Name, Color, Hidden, Xpub58, CreationTime)
    object ProjectionIndex {
      val Index = 0
      val Name = 1
      val Color = 2
      val Hidden = 3
      val Xpub58 = 4
      val CreationTime = 5
    }
  }

  object BlockTableColumns extends BaseColumns {
    val Hash = "block_hash"
    val Height = "block_height"
    val Time = "block_time"

    val projection = Array(Hash, Height, Time)
    object ProjectionIndex {
      val Hash = 0
      val Height = 1
      val Time = 2
    }
  }

  object TransactionTableColumns extends BaseColumns {
    val Hash = "hash"
    val Fees = "fees"
    val Time = "time"
    val LockTime = "lock_time"
    val BlockHash = "block_hash"

    val projection = Array(Hash, Fees, Time, LockTime, BlockHash)
    object ProjectionIndex {
      val Hash = 0
      val Fees = 2
      val Time = 3
      val LockTime = 4
      val BlockHash = 5
    }
  }

  object OperationTableColumns extends BaseColumns {
    val Uid = "uid"
    val AccountId = "account_id"
    val TransactionHash = "transaction_hash"
    val Type = "operation_type"
    val Value = "value"
    val Senders = "senders"
    val Recipients = "recipients"

    val projection = Array(Uid, AccountId, TransactionHash, Type, Value)
    object ProjectionIndex {
      val Uid = 0
      val AccountId = 1
      val TransactionHash = 2
      val Type = 3
      val Value = 4
    }

    object FullOperationProjection {

      object Keys {
        val Uid = s"$OperationTableName.${OperationTableColumns.Uid}"
        val AccountIndex = s"$OperationTableName.${OperationTableColumns.AccountId}"
        val TransactionHash = s"$OperationTableName.${OperationTableColumns.TransactionHash}"
        val Type = s"$OperationTableName.${OperationTableColumns.Type}"
        val Value = s"$OperationTableName.${OperationTableColumns.Value}"
        val Senders = s"$OperationTableName.${OperationTableColumns.Senders}"
        val Recipients = s"$OperationTableName.${OperationTableColumns.Recipients}"
        val AccountName = s"$AccountTableName.${AccountTableColumns.Name}"
        val AccountColor = s"$AccountTableName.${AccountTableColumns.Color}"
        val TransactionFees = s"$TransactionTableName.${TransactionTableColumns.Fees}"
        val TransactionTime = s"$TransactionTableName.${TransactionTableColumns.Time}"
        val TransactionLockTime = s"$TransactionTableName.${TransactionTableColumns.LockTime}"
        val BlockHash = s"$TransactionTableName.${TransactionTableColumns.BlockHash}"
        val BlockHeight = s"$BlockTableName.${BlockTableColumns.Height}"

        val TransactionJoinKey = s"$TransactionTableName.${TransactionTableColumns.Hash}"
        val AccountJoinKey = s"$AccountTableName.${AccountTableColumns.Index}"
        val BlockJoinKey = s"$BlockTableName.${BlockTableColumns.Hash}"

      }

      val allFieldProjectionKeys = Array(
        Keys.Uid, Keys.AccountIndex, Keys.TransactionHash, Keys.Type,
        Keys.Value, Keys.Senders, Keys.Recipients, Keys.AccountName,
        Keys.AccountColor, Keys.TransactionFees, Keys.TransactionTime,
        Keys.TransactionLockTime, Keys.BlockHash, Keys.BlockHeight
      )

      object AllFieldsProjectionIndex {
        val Uid = 0
        val AccountIndex = 1
        val TransactionHash = 2
        val Type = 3
        val Value = 4
        val Senders = 5
        val Recipients = 6
        val AccountName = 7
        val AccountColor = 8
        val TransactionFees = 9
        val TransactionTime = 10
        val TransactionLockTime = 11
        val BlockHash = 12
        val BlockHeight = 3
      }


    }

    object Types {
      val Reception = 0x01
      val Send = 0x02
    }

  }

  object InputTableColumns extends BaseColumns {
    val Uid = "uid"
    val Index = "idx"
    val Path = "path"
    val Value = "value"
    val Coinbase = "coinbase"
    val PreviousTx = "previous_tx"
    val ScriptSig = "script_sig"
    val Address = "address"

    val projection = Array(Uid, Index, Path, Value, Coinbase, PreviousTx, ScriptSig,
      Address)
    object ProjectionIndex {
      val Uid = 0
      val Index = 1
      val Path = 2
      val Value = 3
      val Coinbase = 4
      val PreviousTx = 5
      val ScriptSig = 6
      val Address = 7
    }
  }

  object OutputTableColumns extends BaseColumns {
    val Uid = "uid"
    val TransactionHash = "transaction_hash"
    val Index = "idx"
    val Path = "path"
    val Value = "value"
    val PubKeyScript = "pk_script"
    val Address = "address"

    val projection = Array(Uid, TransactionHash, Index, Path, Value, PubKeyScript, Address)
    object ProjectionIndex {
      val Uid = 0
      val TransactionHash = 1
      val Index = 2
      val Path = 3
      val Value = 4
      val PubKeyScript = 5
      val Address = 6
    }
  }

  object TransactionsInputsTableColumns extends BaseColumns {
    val TransactionHash = "transaction_hash"
    val InputUid = "input_uid"
  }

}
