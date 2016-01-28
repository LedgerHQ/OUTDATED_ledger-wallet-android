/**
 *
 * RichParcel
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/16.
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
package co.ledger.wallet.core.os

import java.util.UUID

import android.os.Parcel

object ParcelExtension {

  implicit class RichParcel(parcel: Parcel) {

    /// Write operations

    def writeNullableString(string: String): Unit = {
      writeNullIndicator(string) {
        parcel.writeString(string)
      }
    }

    def writeNullableUuid(uuid: UUID): Unit = {
      writeNullIndicator(uuid) {
        writeUuid(uuid)
      }
    }

    def writeUuid(uuid: UUID): Unit = {
      parcel.writeString(uuid.toString)
    }

    private def writeNullIndicator(ref: AnyRef)(andThen: => Unit): Unit = {
      if (ref != null) {
        parcel.writeByte(0x01)
        andThen
      } else
        parcel.writeByte(0x00)
    }

    /// Read operations

    def readNullableString(): Option[String] = {
      readNullIndicator {
        parcel.readString()
      }
    }

    def readNullableUuid(): Option[UUID] = {
      readNullIndicator {
        readUuid()
      }
    }

    def readUuid(): UUID = {
      UUID.fromString(parcel.readString())
    }

    private def readNullIndicator[A](andThen: => A): Option[A] = {
      val isNull = parcel.readByte()
      if (isNull == 0x01) {
        Option(andThen)
      } else {
        None
      }
    }

  }

}