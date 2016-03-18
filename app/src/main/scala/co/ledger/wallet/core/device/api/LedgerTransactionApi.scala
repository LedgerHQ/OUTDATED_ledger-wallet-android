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

import co.ledger.wallet.core.device.api.LedgerCommonApiInterface.CommandResult
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
    utxo.transaction.flatMap {(transaction) =>
      writer.writeInt(utxo.outputIndex)
      writer.writeLeInt(transaction.getVersion)
      writer.writeVarInt(transaction.getInputs.size())
      sendApdu(0xe0, 0x42, 0x00, 0x00, writer.toByteArray, 0x00) flatMap { (result) =>
        matchErrorsAndThrow(result)
        // Write each input
        foreach(transaction.getInputs.asScala.toArray) { (input) =>
          val promise = Promise[Null]()
          val writer = new BytesWriter()
          writer.writeReversedByteArray(input.getOutpoint.getHash.getBytes)
          writer.writeLeInt(input.getOutpoint.getIndex)
          writer.writeVarInt(input.getScriptBytes.length)
          sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00) flatMap { (result) =>
            matchErrorsAndThrow(result)
            val writer = new BytesWriter()
            writer.writeByteArray(input.getScriptBytes)
            val sequence = new BytesWriter(8)
            sequence.writeLeInt(input.getSequenceNumber)
            sendApduSplit2(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, sequence.toByteArray, Array(0x9000))
          } map { (result) =>
            matchErrorsAndThrow(result)
            null
          }
        }
      } flatMap { (_) =>
        // Write number of outputs
        val writer = new BytesWriter()
        writer.writeVarInt(transaction.getOutputs.size())
        sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00)
      } flatMap { (r) =>
        matchErrorsAndThrow(r)
        // Write each output
        foreach(transaction.getOutputs.asScala.toArray) { (output) =>
          val writer = new BytesWriter()
          writer.writeLeLong(output.getValue.getValue)
          writer.writeVarInt(output.getScriptBytes.length)
          sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00) flatMap { (r) =>
            matchErrorsAndThrow(r)
            val writer = new BytesWriter()
            writer.writeByteArray(output.getScriptBytes)
            sendApduSplit(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, Array(0x9000))
          } map { (r) =>
            matchErrorsAndThrow(r)
            null
          }
        }
      } flatMap { (_) =>
        val writer = new BytesWriter()
        writer.writeLeLong(transaction.getLockTime)
        sendApdu(0xe0, 0x42, 0x80, 0x00, writer.toByteArray, 0x00)
      } map { (result) =>
        matchErrorsAndThrow(result)
        new Input(result.data, true)
      }
    }
  }

  def startUntrustedTransaction(newTransaction: Boolean,
                                inputIndex: Long,
                                usedInputList: Array[Input],
                                redeemScript: Array[Byte]): Future[Unit] = $("START UNTRUSTED TRANSACTION") {
    import co.ledger.wallet.core.utils.FutureExtensions._
    val writer = new BytesWriter()
    writer.writeLeInt(TransactionVersion)
    writer.writeVarInt(usedInputList.length)
    sendApdu(0xe0, 0x44, 0x00, if (newTransaction) 0x00 else 0x80, writer.toByteArray, 0x00) flatMap {
      (r) =>
        matchErrorsAndThrow(r)
        var currentIndex = 0
        foreach(usedInputList) {(input) =>
          val script = if (currentIndex == inputIndex) redeemScript else Array.empty[Byte]
          currentIndex += 1
          val writer = new BytesWriter()
          writer.writeByte(if (input.isTrusted) 0x01 else 0x00)
          if (input.isTrusted) {
            writer.writeByte(input.value.length)
          }
          writer.writeByteArray(input.bytes)
          writer.writeVarInt(script.length)
          Logger.d(s"Write value ${HexUtils.bytesToHex(writer.toByteArray)}")("APDU")
          sendApdu(0xe0, 0x44, 0x80, 0x00, writer.toByteArray, 0x00) flatMap {(result) =>
            matchErrorsAndThrow(result)
            val writer = new BytesWriter()
            writer.writeByteArray(script)
            writer.writeLeInt(DefaultSequence)
            Logger.d(s"Write REDEEM ${HexUtils.bytesToHex(writer.toByteArray)}")("APDU")
            sendApduSplit(0xe0, 0x44, 0x80, 0x00, writer.toByteArray, Ok)
          }
        }
    } map {(_) =>
      ()
    }
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
                    changePath: DerivationPath): Future[Output] = $("FINALIZE INPUT") {
    val writer = new BytesWriter()
    writer.writeByte(outputAddress.toString.length)
    writer.writeByteArray(outputAddress.toString.getBytes)
    writer.writeLong(amount.getValue)
    writer.writeLong(fees.getValue)
    writer.writeDerivationPath(changePath)
    sendApdu(0xe0, 0x46, 0x02, 0x00, writer.toByteArray, 0x00).map {(result) =>
      matchErrorsAndThrow(result)
      Output(result.data)
    }
  }

  def finalizeInputFull(data: Array[Byte],
                        changePath: Option[DerivationPath],
                        skipChangeCheck: Boolean): Future[Output] = {
    {
      if (!skipChangeCheck && changePath.isDefined) {
        val path = new BytesWriter()
        path.writeDerivationPath(changePath.get)
        sendApdu(0xe0, 0x4a, 0xff, 0x00, path.toByteArray, 0x00) map {(result) =>
          result.sw == 0x6B00 || result.sw == 0x6A86 || result.sw == 0x6985
        }
      } else if (!skipChangeCheck && changePath.isDefined) {
        sendApdu(0xe0, 0x4a, 0xff, 0x00, Array(0x00.toByte), 0x00) map {(result) =>
          result.sw == 0x6B00 || result.sw == 0x6A86 || result.sw == 0x6985
        }
      } else {
        Future.successful(false)
      }
    } flatMap {(oldApi) =>

      def iterate(offset: Int): Future[Output] = {
        val blockLength = if (data.length - offset > 255) 255 else data.length - offset
        val p1 = if (offset + blockLength >= data.length) 0x80 else 0x00
        val p2 = 0x00
        sendApdu(0xe0, 0x4a, p1, p2, data.slice(offset, offset + blockLength), 0x00) flatMap { (result) =>
          matchErrorsAndThrow(result)
          if (offset + blockLength < data.length) {
            iterate(offset + blockLength)
          } else
            Future.successful(Output(result.data))
        }
      }

      iterate(0)
    }
  }
  /*
   public BTChipOutput finalizeInputFull(byte[] data, String changePath, boolean skipChangeCheck) throws BTChipException {
      BTChipOutput result = null;
      int offset = 0;
      byte[] response = null;
      byte[] path = null;
      boolean oldAPI = false;
      if (!skipChangeCheck) {
         if (changePath != null) {
            path = BIP32Utils.splitPath(changePath);
            exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte) 0xFF, (byte) 0x00, path, null);
            oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
         } else {
            exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte) 0xFF, (byte) 0x00, new byte[1], null);
            oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2));
         }
      }
      while (offset < data.length) {
         int blockLength = ((data.length - offset) > 255 ? 255 : data.length - offset);
         byte[] apdu = new byte[blockLength + 5];
         apdu[0] = BTCHIP_CLA;
         apdu[1] = BTCHIP_INS_HASH_INPUT_FINALIZE_FULL;
         apdu[2] = ((offset + blockLength) == data.length ? (byte) 0x80 : (byte) 0x00);
         apdu[3] = (byte) 0x00;
         apdu[4] = (byte) (blockLength);
         System.arraycopy(data, offset, apdu, 5, blockLength);
         response = exchangeCheck(apdu, OK);
         offset += blockLength;
      }
      if (oldAPI) {
         byte value = response[0];
         if (value == UserConfirmation.NONE.getValue()) {
            result = new BTChipOutput(new byte[0], UserConfirmation.NONE);
         } else if (value == UserConfirmation.KEYBOARD.getValue()) {
            result = new BTChipOutput(new byte[0], UserConfirmation.KEYBOARD);
         }
      } else {
         result = convertResponseToOutput(response);
      }
      if (result == null) {
         throw new BTChipException("Unsupported user confirmation method");
      }
      return result;
   }
   */

  def finalizeInput(outputScript: Array[Byte],
                    outputAddress: Address,
                    amount: Coin,
                    fees: Coin,
                    changePath: DerivationPath,
                    requireChange: Boolean): Future[Output] = {
    {
      if (requireChange) {
        val path = new BytesWriter()
        path.writeDerivationPath(changePath)
        sendApdu(0xe0, 0x4a, 0xFF, 0x00, path.toByteArray, 0x00) map {(result) =>
          result.sw == 0x6B00 || result.sw == 0x6A86 || result.sw == 0x6985
        }
      } else {
        sendApdu(0xe0, 0x4a, 0xFF, 0x00, Array(0x00.toByte), 0x00) map {(result) =>
          result.sw == 0x6B00 || result.sw == 0x6A86 || result.sw == 0x6985
        }
      }
    } flatMap {(oldApi) =>
      if (oldApi)
        finalizeInput(outputAddress, amount, fees, changePath)
      else
        finalizeInputFull(outputScript, None, true)
    }
  }

  /*
// Try the new API first
      boolean oldAPI;
      byte[] path = null;
      if (changePath != null) {
         path = BIP32Utils.splitPath(changePath);
         resolvePath(changePath);
         exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte) 0xFF, (byte) 0x00, path, null);
         oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2) || (lastSW == SW_CONDITIONS_NOT_SATISFIED));
      } else {
         exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_INPUT_FINALIZE_FULL, (byte) 0xFF, (byte) 0x00, new byte[1], null);
         oldAPI = ((lastSW == SW_INCORRECT_P1_P2) || (lastSW == SW_WRONG_P1_P2) || (lastSW == SW_CONDITIONS_NOT_SATISFIED));
      }
      if (oldAPI) {
         return finalizeInput(outputAddress, amount, fees, changePath);
      } else {
         return finalizeInputFull(outputScript, null, true);
      }
   */

  def untrustedHashSign(privateKeyPath: DerivationPath, pin: Array[Byte], lockTime: Long = 0,
                        sigHashType: Byte = 0x01): Future[Array[Byte]] = {
    val writer = new BytesWriter()
    writer.writeDerivationPath(privateKeyPath)
    writer.writeByte(pin.length)
    writer.writeByteArray(pin)
    writer.writeInt(lockTime)
    writer.writeByte(sigHashType)
    sendApdu(0xe0, 0x48, 0x00, 0x00, writer.toByteArray, 0x00).map {(result) =>
      matchErrorsAndThrow(result)
      val signature = result.data.readNextBytesUntilEnd()
      signature(0) = 0x30.toByte
      signature
    }
  }

  /*
  	public byte[] untrustedHashSign(String privateKeyPath, byte[] pin, long lockTime, byte sigHashType) throws BTChipException {
		ByteArrayOutputStream data = new ByteArrayOutputStream();
		byte path[] = BIP32Utils.splitPath(privateKeyPath);
		BufferUtils.writeBuffer(data, path);
		data.write(pin.length);
		BufferUtils.writeBuffer(data, pin);
		BufferUtils.writeUint32BE(data, lockTime);
		data.write(sigHashType);
		byte[] response = exchangeApdu(BTCHIP_CLA, BTCHIP_INS_HASH_SIGN, (byte)0x00, (byte)0x00, data.toByteArray(), OK);
		response[0] = (byte)0x30;
		return response;
	}

	public byte[] untrustedHashSign(String privateKeyPath, String pin) throws BTChipException {
		return untrustedHashSign(privateKeyPath, pin.getBytes(), 0, (byte)0x01);
	}

	public byte[] untrustedHashSign(String privateKeyPath, byte[] pin) throws BTChipException {
		return untrustedHashSign(privateKeyPath, pin, 0, (byte)0x01);
	}

   */

}

object LedgerTransactionApi {

  val TransactionVersion = 0x01
  val DefaultSequence = 0xFFFFFFFF

  class Input(val value: BytesReader, val isTrusted: Boolean) {

    val bytes = value.readNextBytesUntilEnd()

  }


  class Output(val value: Array[Byte], val validation: Option[SignatureValidationRequest]) {

    def needsValidation = validation.isDefined

  }

  object Output {

    def apply(reader: BytesReader): Output = {
      val length = reader.readNextByte().toInt
      val value = reader.readNextBytes(length)
      val userValidationMode = reader.readNextByte().toInt
      import ValidationMode._
      userValidationMode match {
        case None => new Output(value, scala.None)// No 2FA
        case Keyboard =>
          val request = new SignatureValidationRequest(Keyboard, reader)
          new Output(value, Some(request))
        case DeprecatedKeycard =>
          val request = new DeprecatedKeyCardSignatureValidationRequest(DeprecatedKeycard, reader)
          new Output(value, Some(request))
        case KeyCardScreen =>
          val request = new KeyCardSignatureValidationRequest(KeyCardScreen, reader)
          new Output(value, Some(request))
        case KeyCard =>
          val request = new KeyCardSignatureValidationRequest(KeyCard, reader)
          new Output(value, Some(request))
        case KeyCardNfc =>
          val request = new KeyCardNfcSignatureValidationRequest(KeyCardNfc, reader)
          new Output(value, Some(request))
        case unsupported =>
          throw Unsupported2FaValidationModeException(unsupported)
      }
    }

  }

  case class Unsupported2FaValidationModeException(mode: Int)
    extends Exception(s"Mode 0x${mode.toHexString} is currently not supported")

  class SignatureNeeds2FAValidationException(val validation: SignatureValidationRequest)
    extends Exception("Signature requires 2FA") {

  }

  class SignatureValidationRequest(val mode: Int,
                                   reader: BytesReader) {

  }


  class DeprecatedKeyCardSignatureValidationRequest(mode: Int,
                                                    reader: BytesReader)
    extends SignatureValidationRequest(mode, reader) {

    val keyCardIndexes = reader.readNextBytesUntilEnd()
    val keyCardChars = keyCardIndexes.map(_ + '0')
  }

  class KeyCardScreenSignatureValidationRequest(mode: Int,
                                                reader: BytesReader)
    extends KeyCardSignatureValidationRequest(mode, reader) {

    val screenInfo = reader.readNextBytesUntilEnd()

  }

  class KeyCardSignatureValidationRequest(mode: Int,
                                          reader: BytesReader)
    extends SignatureValidationRequest(mode, reader) {

    val keyCardIndexesLength = reader.readNextByte().toInt
    val keyCardIndexes = reader.readNextBytes(keyCardIndexesLength)
    def characters(string: String): String = {
      keyCardIndexes.map(string(_)).mkString("")
    }
  }


  class KeyCardNfcSignatureValidationRequest(mode: Int,
                                             reader: BytesReader)
    extends KeyCardSignatureValidationRequest(mode, reader) {

  }

  object ValidationMode {
    val None = 0x00
    val Keyboard = 0x01
    val DeprecatedKeycard = 0x02
    val KeyCardScreen = 0x03
    val KeyCard = 0x04
    val KeyCardNfc = 0x05
  }

}