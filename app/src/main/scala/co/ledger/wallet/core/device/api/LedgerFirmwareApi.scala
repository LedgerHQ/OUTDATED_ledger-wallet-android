/**
 *
 * LedgerFirmwareApi
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

import java.security.{Signature, KeyFactory, SecureRandom}

import co.ledger.wallet.app.Config
import co.ledger.wallet.core.crypto.Crypto
import co.ledger.wallet.core.device.DeviceManager.ConnectivityTypes
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.utils.{BytesReader, HexUtils}
import org.spongycastle.jce.ECNamedCurveTable
import org.spongycastle.jce.interfaces.ECPublicKey
import org.spongycastle.jce.spec.ECPublicKeySpec

import scala.concurrent.Future

trait LedgerFirmwareApi extends LedgerCommonApiInterface {
  import LedgerFirmwareApi._

  def firmwareVersion(): Future[Version] = $$("GET FIRMWARE VERSION") {
    sendApdu(0xE0, 0xC4, 0x00, 0x00, Array(0x00.toByte), 0x08).map({(result) =>
      matchErrorsAndThrow(result)
      new Version(result.data)
    })
  }

  def currentOperationMode = 1

  def deviceAttestation(): Future[Attestation] = $$("GET DEVICE ATTESTATION") {
    val secureRandom = new SecureRandom()
    val random = new Array[Byte](8)
    secureRandom.nextBytes(random)
    // Bypass attestation for Ledger Unplugged
    if (device.connectivityType == ConnectivityTypes.Nfc) {
      Future.successful(attestation(2, 1).get)
    } else {
      sendApdu(0xE0, 0xC2, 0x00, 0x00, 0x08, random, 0x00) map {(result) =>
        val reader = result.data
        val batchId = reader.readNextInt()
        val derivationIndex = reader.readNextInt()
        val version = reader.readNextBytes(8)
        val signedRandom = reader.readNextBytesUntilEnd()
        val attestation = LedgerFirmwareApi.attestation(batchId, derivationIndex).getOrElse {
          throw UnsupportedAttestationException(batchId, derivationIndex)
        }
        if (!attestation.check(version ++ random, signedRandom)) {
          // Check if it is the dev attestation
          if (!LedgerFirmwareApi.attestation(0, 1).get.check(version ++ random, signedRandom))
            throw RogueFirmwareException(batchId, derivationIndex)
        }
        attestation
      }
    }
  }

}

object LedgerFirmwareApi {

  case class Attestation(batchId: Int, derivationIndex: Int, pubKey: Array[Byte]) {

    Crypto.ensureSpongyIsInserted()

    val ecPubKey: ECPublicKey = {
      val factory = KeyFactory.getInstance("ECDH", "SC")
      val ecSpec = ECNamedCurveTable.getParameterSpec("secp256k1")
      val point = ecSpec.getCurve.decodePoint(pubKey)
      val spec = new ECPublicKeySpec(point, ecSpec)
      factory.generatePublic(spec).asInstanceOf[ECPublicKey]
    }

    Crypto.ensureSpongyIsRemoved()

    def check(blob: Array[Byte], signedBlob: Array[Byte]): Boolean = {
      Logger.d(s"<> ${HexUtils.bytesToHex(signedBlob)}")("Toto", false)
      Crypto.ensureSpongyIsInserted()
      val sig = Signature.getInstance("SHA256withECDSA", "SC")
      sig.initVerify(ecPubKey)
      sig.update(blob)
      val result = sig.verify(0x30.toByte +: signedBlob.drop(1))
      Crypto.ensureSpongyIsRemoved()
      result
    }

    override def toString: String = s"(BATCH ID: $batchId, DERIVATION INDEX: $derivationIndex)"
  }

  val Attestations = {
    val array = new Array[Attestation](Config.LedgerAttestationsPublicKeys.size)
    var index = 0
    Config.LedgerAttestationsPublicKeys foreach {
      case ((batchId, derivationIndex), pubKey) =>
        array(index) = Attestation(batchId, derivationIndex, HexUtils.decodeHex(pubKey))
        index += 1
    }
    array
  }

  def attestation(batchId: Int, derivationIndex: Int): Option[Attestation] = {
    Attestations find {(attestation) =>
      attestation.batchId == batchId && attestation.derivationIndex == derivationIndex
    }
  }

  class Version(reader: BytesReader) {

    val features = reader.readNextByte()
    val architecture = reader.readNextByte()
    val major = reader.readNextByte()
    val minor = reader.readNextByte()
    val patch = reader.readNextByte()
    val loaderId = (reader.readNextByte(), reader.readNextByte())
    val wtf = reader.readNextByte()

    val raw = reader.bytes

    val intVersion = architecture.toInt << 24 | major.toInt << 16 | minor.toInt << 8 | patch.toInt

    def usesDeprecatedFirmware = intVersion < FirmwareVersions.BTChip1_4_7
    def usesDeprecatedBip32Derivation = intVersion < FirmwareVersions.BTChip1_4_7
    def arePublicKeysCompressed = hasFeature(0x01)
    def hasBleSupport = hasFeature(0x10)
    def hasSecureScreenAndButton = hasFeature(0x02)
    def hasUnsafeScreenAndButton = hasFeature(0x04)

    def hasScreenAndButton = hasSecureScreenAndButton || hasUnsafeScreenAndButton

    def hasFeature(featureFlag: Int) = (features & featureFlag) == featureFlag

    override def toString: String = s"$major.$minor.$patch"
  }

  class OperationMode(reader: BytesReader) {

    val supportedModesFlag = reader.readNextByte()
    val currentMode = reader.readNextByte()

    override def toString: String = s"(OPERATION MODE: $currentMode, SUPPORTED: $supportedModesFlag)"
  }

  case class RogueFirmwareException(batchId: Int, derivationIndex: Int)
    extends Exception(s"Unable to check attestation $batchId $derivationIndex")

  case class UnsupportedAttestationException(batchId: Int, derivationIndex: Int)
    extends Exception(s"This app doesn't handle attestation $batchId $derivationIndex")

}