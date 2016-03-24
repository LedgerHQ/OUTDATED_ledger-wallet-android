/**
  *
  * ApiAccount
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 17/03/16.
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
package co.ledger.wallet.service.wallet.api

import java.io.{BufferedInputStream, FileInputStream, FileReader, File}

import co.ledger.wallet.core.utils.Preferences
import co.ledger.wallet.core.utils.logs.{Logger, Loggable}
import co.ledger.wallet.service.wallet.AbstractDatabaseStoredAccount
import co.ledger.wallet.service.wallet.database.model.AccountRow
import co.ledger.wallet.wallet.ExtendedPublicKeyProvider
import co.ledger.wallet.wallet.api.ApiWalletClientProtos
import com.google.protobuf.nano.CodedInputByteBufferNano
import org.bitcoinj.core.Transaction
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.wallet.{Protos, DeterministicKeyChain}
import scala.collection.JavaConversions._

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.Future

class ApiAccountClient(val wallet: ApiWalletClient, row: AccountRow)
  extends AbstractDatabaseStoredAccount(wallet) with Loggable {

  override def keyChain: DeterministicKeyChain = _keychain

  override def rawTransaction(hash: String): Future[Transaction] = ???

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] = wallet
    .synchronize(provider)

  def synchronize(syncToken: String): Future[Unit] = Future {
    Logger.d(s"Oh boy, account $index is synchronizing")("Toto", false)

  }

  override def xpub(): Future[DeterministicKey] = Future.successful(_xpub)

  override def index: Int = row.index

  def load(): Future[ApiWalletClientProtos.ApiAccountClient] = Future {
    val input = CodedInputByteBufferNano.newInstance()
    ApiWalletClientProtos.ApiAccountClient.parseFrom(input)
    null
  }

  def save(): Future[Unit] = Future {

  }

  private val _preferences = Preferences(s"ApiAccountClient_$index")(wallet.context)
  private val _xpub = {
    val accountKey = DeterministicKey.deserializeB58(row.xpub58, wallet.networkParameters)
    new DeterministicKey(
      DeterministicKeyChain.ACCOUNT_ZERO_PATH,
      accountKey.getChainCode,
      accountKey.getPubKeyPoint,
      null, null)
  }

  val directory = new File(wallet.directory, s"account_$index/")
  directory.mkdirs()

  private val _keychain = {
    val keychainFile = new File(directory, "keychain")
    if (!keychainFile.exists()) {
      DeterministicKeyChain.watch(_xpub)
    } else {
      try {
        val reader = new BufferedInputStream(new FileInputStream(keychainFile))
        var size = 0
        val keys = new ArrayBuffer[Protos.Key]()
        def readSize(): Int = {
          val b1 = reader.read()
          val b2 = reader.read()
          if (b1 == -1 || b2 == -1) -1 else b1 << 8 | b2
        }
        while ({size = readSize(); size} != -1) {
          val bytes = new Array[Byte](size)
          reader.read(bytes, 0, bytes.length)
          keys += Protos.Key.parseFrom(bytes)
        }
        DeterministicKeyChain.fromProtobuf(keys.toList, null, null).get(0)
      } catch {
        case anything: Throwable =>
          anything.printStackTrace()
          DeterministicKeyChain.watch(_xpub)
      }
    }
  }

  private val savedStateFile = new File(directory, "saved_state")

}
