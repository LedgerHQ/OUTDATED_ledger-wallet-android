/**
 *
 * LedgerTransactionEasyApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/02/16.
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
package co.ledger.wallet.core.device.api

import co.ledger.wallet.wallet.{DerivationPath, Utxo}
import org.bitcoinj.core.{Address, Coin, Transaction}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.{ExecutionContext, Future}

trait LedgerTransactionEasyApi extends LedgerTransactionApi {
  import LedgerTransactionApi._

  def buildTransaction(): TransactionBuilder = new TransactionBuilder

  class TransactionBuilder {

    // Configuration

    def from(utxo: Array[Utxo]): TransactionBuilder = {
      _utxo ++= utxo
      this
    }

    def from(utxo: Utxo): TransactionBuilder = {
      _utxo += utxo
      this
    }

    def to(address: Address, value: Coin): TransactionBuilder = {
      _to += address -> value
      this
    }

    def fees(fees: Coin): TransactionBuilder = {
      _fees = Option(fees)
      this
    }

    def change(path: DerivationPath): TransactionBuilder = {
      _change = Option(path)
      this
    }

    def complete2FA(answer: Array[Byte]): TransactionBuilder = {
      _2faAnswer = Option(answer)
      this
    }

    def onProgress(handler: (Int, Int) => Unit)(implicit ec: ExecutionContext): Unit = {
      _progressHandler = Option(handler -> ec)
    }

    // Signature

    def sign(): Future[Transaction] = {
      if (_changeValue.isEmpty)
        computeChangeValue()
      else if (_trustedInputs.isEmpty)
        fetchTrustedInputs()
      else if (_signatures.isEmpty)
        signInputs()
      else
        buildTransaction()
    }

    private def computeChangeValue(): Future[Transaction] = {
      require(_fees.isDefined, "You must set fees before signing")
      require(_change.isDefined, "You must set a change before signing")
      require(_utxo.nonEmpty, "You must use at least one UTXO")
      require(_to.nonEmpty, "You must have at least one output")
      val changeValue =
        _utxo.map(_.value).fold(Coin.ZERO)(_ add _) subtract
          _to.map(_._2).fold(Coin.ZERO)(_ add _) subtract _fees.get
      require(changeValue.isPositive, "Not enough funds")
      _changeValue = Some(changeValue)
      sign()
    }

    private def fetchTrustedInputs(): Future[Transaction] = {
      var trustedInputs = new ArrayBuffer[Input]()
      def iterate(index: Int): Future[Any] = {
        getTrustedInput(_utxo(index)) flatMap {(input) =>
          trustedInputs += input
          if (index + 1 < _utxo.length) {
            iterate(index + 1)
          } else {
            Future.successful(null)
          }
        }
      }
      iterate(0) flatMap {(_) =>
        _trustedInputs = Option(trustedInputs.toArray)
        sign()
      }
    }

    private def signInputs(): Future[Transaction] = {
      null
    }

    private def buildTransaction(): Future[Transaction] = {
      null
    }

    private def needsChangeOutput = _changeValue.exists(!_.isZero)

    // Configurable
    private var _progressHandler: Option[((Int, Int) => Unit, ExecutionContext)] = None
    private var _fees: Option[Coin] = None
    private var _utxo: ArrayBuffer[Utxo] = new ArrayBuffer[Utxo]()
    private var _to: ArrayBuffer[(Address, Coin)] = new ArrayBuffer[(Address, Coin)]()
    private var _change: Option[DerivationPath] = None
    private var _2faAnswer: Option[Array[Byte]] = None

    // Progression
    private var _changeValue: Option[Coin] = None
    private var _trustedInputs: Option[Array[Input]] = None
    private var _signatures: Array[Array[Byte]] = Array()
  }


}
