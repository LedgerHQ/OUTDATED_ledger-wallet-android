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

import co.ledger.wallet.core.utils.{HexUtils, BytesWriter}
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.wallet.{DerivationPath, Utxo}
import com.btchip.utils.SignatureUtils
import org.bitcoinj.core._
import org.bitcoinj.params.MainNetParams
import org.bitcoinj.script.{ScriptBuilder, Script}

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

    def change(path: DerivationPath, address: Address): TransactionBuilder = {
      _change = Option((path, address))
      this
    }

    def complete2FA(answer: Array[Byte]): TransactionBuilder = {
      _2faAnswer = Option(answer)
      this
    }

    var networkParameters: NetworkParameters = MainNetParams.get()

    def onProgress(handler: (Int, Int) => Unit)(implicit ec: ExecutionContext): Unit = {
      _progressHandler = Option(handler -> ec)
    }

    def notifyProgress(): Unit = {
      _currentProgress += 1
      _progressHandler foreach {
        case (handler, ec) =>
          ec.execute(new Runnable {
            override def run(): Unit = handler(_currentProgress, _maxProgress)
          })
      }
    }

    private var _signatureValidationRequest: Option[SignatureValidationRequest] = None
    def signatureValidationRequest = _signatureValidationRequest

    // Signature

    def sign(): Future[Transaction] = {
      if (_maxProgress == -1)
        computeMaxProgress()
      else if (_changeValue.isEmpty)
        computeChangeValue()
      else if (_rawOutputs.isEmpty)
        prepareOutputs()
      else if (_trustedInputs.isEmpty)
        fetchTrustedInputs()
      else if (_signatures.isEmpty)
        signInputs()
      else
        buildTransaction()
    }

    private def computeMaxProgress(): Future[Transaction] = {
      _maxProgress =
        _utxo.length + // Fetch trusted inputs
        _utxo.length + // Start untrusted hash signature
        _utxo.length + // Finalize
        _utxo.length + // Sign
        1 // Build
      sign()
    }

    private def computeChangeValue(): Future[Transaction] = Future {
      require(_fees.isDefined, "You must set fees before signing")
      require(_change.isDefined, "You must set a change before signing")
      require(_utxo.nonEmpty, "You must use at least one UTXO")
      require(_to.nonEmpty, "You must have at least one output")
      val changeValue =
        _utxo.map(_.value).fold(Coin.ZERO)(_ add _) subtract
          _to.map(_._2).fold(Coin.ZERO)(_ add _) subtract _fees.get
      require(changeValue.isPositive, "Not enough funds")
      _changeValue = Some(changeValue)
    } flatMap {(_) =>
      sign()
    }

    private def prepareOutputs(): Future[Transaction] = {
      createRawOutputs(false)
      sign()
    }

    private def createRawOutputs(inverse: Boolean): Unit = {
      def writeOutput(writer: BytesWriter, output: (Address, Coin)) = {
        writer.writeLeLong(output._2.getValue)
        val script = ScriptBuilder.createOutputScript(output._1)
        writer.writeVarInt(script.getProgram.length)
        writer.writeByteArray(script.getProgram)
      }

      val writer = new BytesWriter()
      val outputsCount = _to.length + (if (needsChangeOutput) 1 else 0)

      writer.writeVarInt(outputsCount)
      if (needsChangeOutput && inverse)
        writeOutput(writer, (_change.get._2, _changeValue.get))
      _to foreach {(pair) =>
        writeOutput(writer, pair)
      }
      if (needsChangeOutput && !inverse)
        writeOutput(writer, (_change.get._2, _changeValue.get))
      _rawOutputs = Option(writer.toByteArray)
    }

    private def fetchTrustedInputs(): Future[Transaction] = {
      var trustedInputs = new ArrayBuffer[Input]()
      def iterate(index: Int): Future[Any] = {
        getTrustedInput(_utxo(index)) flatMap {(input) =>
          trustedInputs += input
          notifyProgress()
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
      val signatures = new ArrayBuffer[Array[Byte]]()
      def iterate(index: Int): Future[Unit] = {
        val utxo = _utxo(index)
        utxo.transaction flatMap {(transaction) =>
          val redeemScript = transaction.getOutput(utxo.outputIndex).getScriptBytes
          startUntrustedTransaction(index == 0 && _2faAnswer.isEmpty, index, _trustedInputs.get, redeemScript) flatMap { (_) =>
            notifyProgress()
            finalizeInput(_rawOutputs.get, _to.head._1, _to.head._2, _fees.get, _change.get._1, needsChangeOutput)
          } flatMap { (output) =>
            if (_output.isEmpty)
              _output = Option(output)
            if (output.needsValidation && _2faAnswer.isEmpty) {
              throw new SignatureNeeds2FAValidationException(output.validation.get)
            }
            notifyProgress()
            untrustedHashSign(utxo.path, _2faAnswer.getOrElse("".getBytes))
          } flatMap { (signature) =>
            notifyProgress()
            signatures += SignatureUtils.canonicalize(signature, true, 0x01)
            if (index + 1 < _utxo.length) {
              iterate(index + 1)
            } else {
              Future.successful()
            }
          }
        }
      }
      iterate(0) flatMap {(_) =>
        _signatures = signatures.toArray
        sign()
      }
    }

    /**
     * Only for old transaction api
     */
    private def swapOutputIfNecessary(): Unit = {
      // Parse the output data
      if (needsChangeOutput && _output.exists(_.value != null) &&
        _output.exists(_.value.length > 0)) {
        val firstOutput = new TransactionOutput(networkParameters, null, _rawOutputs.get, 1)
        val dongleOutput = new TransactionOutput(networkParameters, null, _output.get.value, 1)
        if (!firstOutput.getValue.equals(dongleOutput.getValue) ||
            firstOutput.getScriptBytes.deep != dongleOutput.getScriptBytes.deep) {
          createRawOutputs(true)
        }
      }
    }

    private def buildTransaction(): Future[Transaction] = {
      swapOutputIfNecessary()
      val signatures = _signatures
      val inputs = _utxo
      val transaction = new BytesWriter()

      Future.sequence(inputs.map(_.transaction)) flatMap {(transactions) =>

        // Version LE Int
        transaction.writeLeInt(TransactionVersion)
        // Input count VI
        transaction.writeVarInt(inputs.length)
        // Inputs
        for (i <- inputs.indices) {
          val input = inputs(i)
          val prevTx = transactions(i)
          // Reversed prev tx hash
          transaction.writeReversedByteArray(prevTx.getHash.getBytes)
          // Previous output index (LE Int)
          transaction.writeLeInt(input.outputIndex)
          // Script Sig
          val scriptSig = new BytesWriter()
          scriptSig.writeByte(signatures(i).length)
          scriptSig.writeByteArray(signatures(i))
          scriptSig.writeByte(input.publicKey.length)
          scriptSig.writeByteArray(input.publicKey)
          transaction.writeVarInt(scriptSig.toByteArray.length)
          transaction.writeByteArray(scriptSig.toByteArray)
          // Sequence (LE int)
          transaction.writeLeInt(DefaultSequence)
        }
        // Outputs count VI
        // Output value (LE long)
        // Script length VI
        // Script
        for (rawOutputs <- _rawOutputs) {
          transaction.writeByteArray(rawOutputs)
        }
        // Block lock time
        transaction.writeLeInt(0x00)
        Logger.d(s"Create ${HexUtils.bytesToHex(transaction.toByteArray)}")("TX")
        notifyProgress()
        Future.successful(new Transaction(networkParameters, transaction.toByteArray))
      }
    }

    private def needsChangeOutput = _changeValue.exists(!_.isZero)

    // Configurable
    private var _progressHandler: Option[((Int, Int) => Unit, ExecutionContext)] = None
    private var _fees: Option[Coin] = None
    private var _utxo: ArrayBuffer[Utxo] = new ArrayBuffer[Utxo]()
    private var _to: ArrayBuffer[(Address, Coin)] = new ArrayBuffer[(Address, Coin)]()
    private var _change: Option[(DerivationPath, Address)] = None
    private var _2faAnswer: Option[Array[Byte]] = None

    // Progression
    private var _changeValue: Option[Coin] = None
    private var _rawOutputs: Option[Array[Byte]] = None
    private var _output: Option[Output] = None
    private var _trustedInputs: Option[Array[Input]] = None
    private var _signatures: Array[Array[Byte]] = Array()

    // Progress notifier
    private var _maxProgress = -1
    private var _currentProgress = 0
  }


}
