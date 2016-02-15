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

import java.io.ByteArrayInputStream

import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.utils.{HexUtils, BytesReader, BytesWriter}
import co.ledger.wallet.wallet.{Utxo, DerivationPath}
import com.btchip.{BitcoinTransaction, BTChipDongle}
import com.btchip.comm.BTChipTransport
import org.bitcoinj.core.{Transaction => JTransaction, Coin, Address}

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Promise, Future}
import scala.collection.JavaConverters._

trait LedgerTransactionApi extends LedgerCommonApiInterface {
  import LedgerTransactionApi._

  def getTrustedInput(utxo: Utxo): Future[Input] = $("GET TRUSTED INPUT") {
    import co.ledger.wallet.core.utils.FutureExtensions._

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
        writer.writeReversedByteArray(input.getOutpoint.getHash.getBytes)
        writer.writeLeInt(input.getOutpoint.getIndex)
        writer.writeVarInt(input.getScriptBytes.length)
        sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00) flatMap {(result) =>
          matchErrorsAndThrow(result)
          val writer = new BytesWriter()
          writer.writeByteArray(input.getScriptBytes)
          val sequence = new BytesWriter(8)
          sequence.writeLeInt(input.getSequenceNumber)
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
  }

  def startUntrustedTransaction(newTransaction: Boolean,
                                inputIndex: Long,
                                usedInputList: Array[Input],
                                redeemScript: Array[Byte]): Future[Unit] = {
    import co.ledger.wallet.core.utils.FutureExtensions._
    val writer = new BytesWriter()
    writer.writeLeInt(TransactionVersion)
    writer.writeVarInt(usedInputList.length)
    sendApdu(0xe0, 0x44, 0x00, if (newTransaction) 0x00 else 0x80, writer.toByteArray, 0x00) flatMap {
      (r) =>
        matchErrorsAndThrow(r)
        foreach(usedInputList) {(input) =>

          null
        }
    }
    null
    /*
    // Start building a fake transaction with the passed inputs
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		BufferUtils.writeBuffer(data, BitcoinTransaction.DEFAULT_VERSION);
		VarintUtils.write(data, usedInputList.length);
		exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x00, (newTransaction ? (byte)0x00 : (byte)0x80), data.toByteArray(), OK);
		// Loop for each input
		long currentIndex = 0;
		for (BTChipInput input : usedInputList) {
			byte[] script = (currentIndex == inputIndex ? redeemScript : new byte[0]);
			data = new ByteArrayOutputStream();
			data.write(input.isTrusted() ? (byte)0x01 : (byte)0x00);
			if (input.isTrusted()) {
				// untrusted inputs have constant length
				data.write(input.getValue().length);
			}
			BufferUtils.writeBuffer(data, input.getValue());
			VarintUtils.write(data, script.length);
			exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			data = new ByteArrayOutputStream();
			BufferUtils.writeBuffer(data, script);
			BufferUtils.writeBuffer(data, BitcoinTransaction.DEFAULT_SEQUENCE);
			exchangeApduSplit(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_START, (byte)0x80, (byte)0x00, data.toByteArray(), OK);
			currentIndex++;
		}
     */
  }

  def finalizeInput(outputAddress: Address,
                    amount: Coin,
                    fees: Coin,
                    changePath: DerivationPath): Future[Array[Byte]] = {
    null
  }

  /*
  public BTChipOutput finalizeInput(String outputAddress, String amount, String fees, String changePath) throws BTChipException {
		BTChipOutput result = null;
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = BIP32Utils.splitPath(changePath);
		data.write(outputAddress.length());
		BufferUtils.writeBuffer(data, outputAddress.getBytes());
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(amount));
		BufferUtils.writeUint64BE(data, CoinFormatUtils.toSatoshi(fees));
		BufferUtils.writeBuffer(data, path);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE, (byte)0x02, (byte)0x00, data.toByteArray(), OK);
		result = convertResponseToOutput(response);
		return result;
	}
   */


}

object LedgerTransactionApi {

  val TransactionVersion = 0x01

  class Input(val value: BytesReader, val isTrusted: Boolean) {

  }

  class Output {

  }

}