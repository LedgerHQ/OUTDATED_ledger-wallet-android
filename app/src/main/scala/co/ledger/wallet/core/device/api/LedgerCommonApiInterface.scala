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
import co.ledger.wallet.core.utils.{BytesReader, BytesWriter}

import scala.concurrent.{ExecutionContext, Future, Promise}

trait LedgerCommonApiInterface extends ParcelableObject {

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
    sendApdu(cla, ins, p1, data.length, data, le)
  }

  protected def sendApdu(command: Array[Byte]): Future[CommandResult] = {
    device.readyForExchange flatMap {(_) =>
      device.exchange(command)
    } map {(result) =>
      new CommandResult(result)
    }
  }

  /** *
    * Schedule a command to run on the device
    * @param name Friendly name to display
    * @param handler The actual body of the method
    * @tparam T
    * @return
    */
  protected def $[T <: AnyRef](name: String)(handler: => Future[T]): Future[T] = {
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
  protected def $$[T <: AnyRef](name: String)(handler: => Future[T]): Future[T] = synchronized {
    if (!_resultCache.contains(name)) {
     _resultCache(name) = $(name)(handler)
    }
    _resultCache(name).asInstanceOf[Future[T]]
  }

  def cancelPendingCommands(): Unit = {
    _tasks.removeAll()
  }

  private[this] def _tasks = new FutureQueue[AnyRef](ec) {
    override protected def onTaskFailed(name: String, cause: Throwable): Unit = {
      super.onTaskFailed(name, cause)
    }
  }

  private[this] val _resultCache = scala.collection.mutable.Map[String, Future[AnyRef]]()
}

object LedgerCommonApiInterface {

  class CommandResult(result: Array[Byte]) {

    private val reader = new BytesReader(result)

    val responseLength = reader.readNextShort()
    val data = reader.slice(2, reader.length - 2)

    reader.seek(data.length - 2)

    val sw = reader.readNextByte().toShort << 8 |reader.readNextByte().toShort
  }

}