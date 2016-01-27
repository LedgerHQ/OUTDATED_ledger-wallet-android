/**
 *
 * LedgerCommonApiInterface
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/01/16.
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

import android.os.Parcel
import co.ledger.wallet.core.concurrent.FutureQueue
import co.ledger.wallet.core.device.Device
import co.ledger.wallet.core.device.api.LedgerCommonApiInterface.CommandResult
import co.ledger.wallet.core.os.ParcelableObject
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.core.utils.{HexUtils, BytesReader, BytesWriter}

import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.util.{Failure, Success, Try}

trait LedgerCommonApiInterface extends ParcelableObject with Loggable {
  import LedgerCommonApiInterface._

  implicit val ec: ExecutionContext
  def device: Device

  abstract override def writeToParcel(dest: Parcel, flags: Int): Unit = {
    super.writeToParcel(dest, flags)
    // Stackable!
  }

  abstract override def readFromParcel(source: Parcel): Unit = {
    super.readFromParcel(source)
    // Stackable!
  }

  /**
   * Send an APDU to the chip
   * @param cla Instruction class
   * @param ins Instruction
   * @param p1 First parameter
   * @param p2 Second parameter
   * @param lc Length of data to send
   * @param data Data to transfer
   * @param le Maximum length to received
   * @return
   */
  protected def sendApdu(cla: Int, ins: Int, p1: Int, p2: Int, lc: Int, data: Array[Byte], le: Int)
  : Future[CommandResult] = {
    val raw = new BytesWriter(6 + data.length)
      .writeByte(cla)
      .writeByte(ins)
      .writeByte(p1)
      .writeByte(p2)
      .writeByte(lc)
      .writeByteArray(data)
      .writeByte(le)
      .toByteArray
    sendApdu(raw)
  }

  /**
   * Send an APDU to the chip (sets lc to data length)
   * @param cla
   * @param ins
   * @param p1
   * @param p2
   * @param data
   * @param le
   * @return
   */
  protected def sendApdu(cla: Int, ins: Int, p1: Int, p2: Int, data: Array[Byte], le: Int)
  : Future[CommandResult] = {
    sendApdu(cla, ins, p1, p2, data.length, data, le)
  }

  protected def sendApdu(cla: Int, ins: Int, p1: Int, p2: Int, lc: Int, le: Int):
  Future[CommandResult] ={
    sendApdu(cla, ins, p1, p2, lc, Array.empty[Byte], le)
  }

  protected def sendApdu(command: Array[Byte]): Future[CommandResult] = {
    device.readyForExchange flatMap {(_) =>
      Logger.d(s"=> ${HexUtils.bytesToHex(command)}")
      device.exchange(command)
    } map {(result) =>
      Logger.d(s"<= ${HexUtils.bytesToHex(result)}")
      new CommandResult(result)
    }
  }

  protected def matchErrors(result: CommandResult): Try[Unit] = {
    if (result.sw == 0x9000)
      Success()
    else {
      result.sw match {
        case 0x6700 => Failure(LedgerApiIncorrectLengthException())
        case 0x6982 => Failure(LedgerApiInvalidAccessRightException())
        case 0x6A80 => Failure(LedgerApiInvalidDataException())
        case 0x6A82 => Failure(LedgerApiFileNotFoundException())
        case 0x6B00 => Failure(LedgerApiInvalidParameterException())
        case 0x6D00 => Failure(LedgerApiNotImplementedException())
        case code: Int =>
          if ((code | 0x6F00) == 0x6F00)
            Failure(LedgerApiTechnicalProblemException(code))
          else
            Failure(LedgerApiUnknownErrorException(code))
      }
    }
  }

  /** *
    * Schedule a command to run on the device
    * @param name Friendly name to display
    * @param handler The actual body of the method
    * @tparam T
    * @return
    */
  protected def $[T <: Any](name: String)(handler: => Future[T]): Future[T] = {
    val promise = Promise[T]()
    val fun = {() =>
      promise.completeWith(handler)
      promise.future
    }
    _tasks.enqueue(fun, name)
    promise.future
  }

  /** *
    * Same as [[LedgerCommonApiInterface.$()]] but caches the result
    * @param name
    * @param handler
    * @tparam T
    * @return
    */
  protected def $$[T <: Any](name: String)(handler: => Future[T]): Future[T] = synchronized {
    if (!_resultCache.contains(name)) {
     _resultCache(name) = $(name)(handler)
    }
    _resultCache(name).asInstanceOf[Future[T]]
  }

  def cancelPendingCommands(): Unit = {
    _tasks.removeAll()
  }

  private[this] def _tasks = new FutureQueue[Any](ec) {
    override protected def onTaskFailed(name: String, cause: Throwable): Unit = {
      super.onTaskFailed(name, cause)
    }
  }

  private[this] val _resultCache = scala.collection.mutable.Map[String, Future[Any]]()
}

object LedgerCommonApiInterface {

  class CommandResult(result: Array[Byte]) {

    private val reader = new BytesReader(result)

    val data = reader.slice(0, reader.length - 2)

    reader.seek(data.length - 2)

    val sw = reader.readNextByte().toShort << 8 |reader.readNextByte().toShort
  }

  class LedgerApiException(code: Int, msg: String)
    extends Exception(s"$msg - ${Integer.toHexString(code)}")
  case class LedgerApiIncorrectLengthException() extends
    LedgerApiException(0x6700, "Incorrect length")
  case class LedgerApiInvalidAccessRightException() extends
    LedgerApiException(0x6982, "Security status not satisfied (Bitcoin dongle is locked or invalid access rights)")
  case class LedgerApiInvalidDataException() extends
    LedgerApiException(0x6A80, "Invalid data")
  case class LedgerApiFileNotFoundException() extends
    LedgerApiException(0x6A82, "File not found")
  case class LedgerApiInvalidParameterException() extends
    LedgerApiException(0x6B00, "incorect parameter P1 or P2")
  case class LedgerApiNotImplementedException() extends
    LedgerApiException(0x6D00, "Not implemented")
  case class LedgerApiTechnicalProblemException(code: Int) extends
    LedgerApiException(code, "Technical problem (Internal error, please report)")
  case class LedgerApiUnknownErrorException(code: Int) extends
    LedgerApiException(code, "Unexpected status word")

}