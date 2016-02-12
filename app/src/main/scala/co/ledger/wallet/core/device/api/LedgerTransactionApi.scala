/**
 *
 * LedgerTransactionApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/02/16.
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

import co.ledger.wallet.core.utils.{BytesReader, BytesWriter}
import co.ledger.wallet.wallet.{Utxo, DerivationPath}
import org.bitcoinj.core.{Transaction => JTransaction, Coin, Address}

import scala.concurrent.{Promise, Future}
import scala.collection.JavaConverters._

trait LedgerTransactionApi extends LedgerCommonApiInterface {
  import LedgerTransactionApi._

  def getTrustedInput(utxo: Utxo): Future[Input] = $("GET TRUSTED INPUT") {
    import co.ledger.wallet.core.utils.FutureExtensions._
    // Write the transaction of the given utxo
    // Write header
    val writer = new BytesWriter()
    writer.writeInt(utxo.outputIndex)
    writer.writeLeInt(utxo.transaction.getVersion)
    writer.writeVarInt(utxo.transaction.getInputs.size())
    sendApdu(0xe0, 0x42, 0x00, 0x00, writer.toByteArray, 0x00) flatMap {(result) =>
      matchErrorsAndThrow(result)
      // Write each input
      foreach(utxo.transaction.getInputs.asScala.toArray) {(input) =>
        val promise = Promise[Null]()
        val writer = new BytesWriter()
        writer.writeByteArray(input.getOutpoint.unsafeBitcoinSerialize())
        writer.writeVarInt(input.getScriptBytes.length)
        sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00) flatMap {(result) =>
          matchErrorsAndThrow(result)
          val writer = new BytesWriter()
          writer.writeByteArray(input.getScriptBytes)
          val sequence = new BytesWriter(8)
          sequence.writeLeLong(input.getSequenceNumber)
          sendApduSplit2(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, sequence.toByteArray, Array(0x9000))
        } map {(result) =>
          matchErrorsAndThrow(result)
          null
        }
      }
    } flatMap {(_) =>
      // Write number of outputs
      val writer = new BytesWriter()
      writer.writeVarInt(utxo.transaction.getOutputs.size())
      sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00)
    } flatMap {(r) =>
      matchErrorsAndThrow(r)
      // Write each output
      foreach(utxo.transaction.getOutputs.asScala.toArray) {(output) =>
        val writer = new BytesWriter()
        writer.writeLeLong(output.getValue.getValue)
        writer.writeVarInt(output.getScriptBytes.length)
        sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00) flatMap {(r) =>
          matchErrorsAndThrow(r)
          val writer = new BytesWriter()
          writer.writeByteArray(output.getScriptBytes)
          sendApduSplit(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, Array(0x9000))
        } map {(r) =>
          matchErrorsAndThrow(r)
          null
        }
      }
    } flatMap {(_) =>
      val writer = new BytesWriter()
      writer.writeLeLong(utxo.transaction.getLockTime)
      sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00)
    } map {(result) =>
      matchErrorsAndThrow(result)
      new Input(result.data, true)
    }
    /*
    ByteArrayOutputStream data = new ByteArrayOutputStream();
		// Header
		BufferUtils.writeUint32BE(data, index);
		BufferUtils.writeBuffer(data, transaction.getVersion());
		VarintUtils.write(data, transaction.getInputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		// Each input
		for (BitcoinTransaction.BitcoinInput input : transaction.getInputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getPrevOut());
			VarintUtils.write(data, input.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, input.getScript());
			exchangeApduSplit2(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), input.getSequence(), OK);
		}
		// Number of outputs
		data = new ByteArrayOutputStream();
		VarintUtils.write(data, transaction.getOutputs().size());
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		// Each output
		for (BitcoinTransaction.BitcoinOutput output : transaction.getOutputs()) {
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getAmount());
			VarintUtils.write(data, output.getScript().length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, output.getScript());
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
		}
		// Locktime
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_GET_TRUSTED_INPUT, (byte)0x80, (byte)0x00, transaction.getLockTime(), OK);
		return new BTChipInput(response, true);
     */
  }

  def startUntrustedTransaction(newTransaction: Boolean,
                                inputIndex: Long,
                                usedInputList: Array[Input],
                                redeemScript: Array[Byte]): Future[Unit] = {
    null
  }

  def finalizeInput(address: Address,
                    amount: Coin,
                    fees: Coin,
                    changePath: DerivationPath): Future[Output] = {
    null
  }

}

object LedgerTransactionApi {

  class Input(val value: BytesReader, val isTrusted: Boolean) {

  }

  class Output {

  }

}