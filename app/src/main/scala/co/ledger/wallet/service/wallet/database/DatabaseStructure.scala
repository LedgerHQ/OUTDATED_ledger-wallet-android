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
  val OperationTableName = "operation"
  val InputTableName = "input"
  val OutputTableName = "output"
  val OperationsInputsTableName = "operations_inputs"

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

  object OperationTableColumns extends BaseColumns {
    val Uid = "uid"
    val AccountId = "account_id"
    val Hash = "hash"
    val Fees = "fees"
    val Time = "time"
    val LockTime = "lock_time"
    val Value = "value"
    val Type = "type"
    val BlockHash = "block_hash"

    val projection = Array(Uid, AccountId, Hash, Fees, Time, LockTime, Value, Type, BlockHash)
    object ProjectionIndex {
      val Uid = 0
      val AccountId = 1
      val Hash = 2
      val Fees = 3
      val Time = 4
      val LockTime = 5
      val Value = 6
      val Type = 7
      val BlockHash = 8
    }
  }

  object InputTableColumns extends BaseColumns {
    val Uid = "uid"
    val Index = "index"
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
      val OperationUId = 1
      val Index = 2
      val Path = 3
      val Value = 4
      val Coinbase = 5
      val PreviousTx = 6
      val ScriptSig = 7
      val Address = 8
    }
  }

  object OutputTableColumns extends BaseColumns {
    val Uid = "uid"
    val TransactionHash = "transaction_hash"
    val Index = "index"
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

  object OperationsInputsTableColumns extends BaseColumns {
    val OperationUid = "operation_uid"
    val InputUid = "input_uid"
  }

}
