/**
 *
 * ECKeyPair
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 04/02/15.
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
package com.ledger.ledgerwallet.crypto

import java.math.BigInteger
import java.security.{Security, SecureRandom}

import org.spongycastle.asn1.sec.SECNamedCurves
import org.spongycastle.crypto.AsymmetricCipherKeyPair
import org.spongycastle.crypto.generators.ECKeyPairGenerator
import org.spongycastle.crypto.params.{ECPrivateKeyParameters, ECPublicKeyParameters, ECDomainParameters, ECKeyGenerationParameters}
import org.spongycastle.math.ec.ECPoint
import org.spongycastle.util.encoders.Hex

abstract class ECKeyPair {

  def publicKeyPoint: ECPoint
  def privateKeyInteger: BigInteger

  def privateKey = privateKeyInteger.toByteArray
  def publicKey = publicKeyPoint.getEncoded
  def compressedPublicKey = new ECPoint.Fp(ECKeyPair.Domain.getCurve, publicKeyPoint.getXCoord, publicKeyPoint.getYCoord, true).getEncoded

  def privateKeyHexString = Hex.toHexString(privateKey)
  def publicKeyHexString = Hex.toHexString(publicKey)
  def compressedPublicKeyHexString = Hex.toHexString(compressedPublicKey)

  def generateAgreementSecret(publicKeyHex: String): Array[Byte] = generateAgreementSecret(Hex.decode(publicKeyHex))

  def generateAgreementSecret(publicKey: Array[Byte]): Array[Byte] = {
    val publicKeyPoint = ECKeyPair.bytesToEcPoint(publicKey)
    val P = publicKeyPoint.multiply(ECKeyPair.Domain.getH.multiply(privateKeyInteger))
    P.getXCoord.getEncoded
  }
}

object ECKeyPair {
  Security.addProvider(new org.spongycastle.jce.provider.BouncyCastleProvider)
  val Curve = SECNamedCurves.getByName("secp256k1")
  val Domain = new ECDomainParameters(Curve.getCurve, Curve.getG, Curve.getN, Curve.getH)
  val SecureRandom = new SecureRandom
  private[this] val generator = new ECKeyPairGenerator
  private[this] val keygenParams = new ECKeyGenerationParameters(ECKeyPair.Domain, ECKeyPair.SecureRandom)
  generator.init(keygenParams)

  def generate(): ECKeyPair = new ECKeyPairFromAsymmetricCipherKeyPair(generator.generateKeyPair())
  def create(privateKey: Array[Byte]): ECKeyPair = new ECKeyPairFromPrivateKey(privateKey)
  def bytesToEcPoint(bytes: Array[Byte]): ECPoint = Curve.getCurve.decodePoint(bytes)

  private sealed class ECKeyPairFromAsymmetricCipherKeyPair(keyPair: AsymmetricCipherKeyPair) extends ECKeyPair {
    def publicKeyParameters = keyPair.getPublic.asInstanceOf[ECPublicKeyParameters]
    def privateKeyParameters = keyPair.getPrivate.asInstanceOf[ECPrivateKeyParameters]

    def publicKeyPoint = publicKeyParameters.getQ
    def privateKeyInteger = privateKeyParameters.getD
  }

  private sealed class ECKeyPairFromPrivateKey(bytes: Array[Byte]) extends ECKeyPair {

    private[this] val _privateKey = new BigInteger(1, bytes).mod(Curve.getN)
    private[this] val _publicKey = Curve.getG.multiply(_privateKey)

    def publicKeyPoint = _publicKey
    def privateKeyInteger = _privateKey
  }

}