/**
 *
 * LedgerDerivationApi
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 26/01/16.
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

import co.ledger.wallet.core.bitcoin.{Base58, BitcoinUtils}
import co.ledger.wallet.core.crypto.{Crypto, Hash160}
import co.ledger.wallet.core.device.api.LedgerDerivationApi.PublicAddressResult
import co.ledger.wallet.core.utils.{AsciiUtils, HexUtils, BytesReader, BytesWriter}
import co.ledger.wallet.wallet.DerivationPath
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams

import scala.concurrent.Future

trait LedgerDerivationApi extends LedgerFirmwareApi {
  import LedgerDerivationApi._
  import LedgerCommonApiInterface._
  /*
  getWalletPublicKey_async: function (path) {
        var data;
        var path = this.parseBIP32Path(path);
        var p1;
        if (this.deprecatedBIP32Derivation) {
            var account, chainIndex, internalChain;
            if (path.length != 3) {
                throw "Invalid BIP 32 path for deprecated BIP32 derivation";
            }
            account = path[0];
            internalChain = (path[1].equals(new ByteString("00000001", HEX)));
            chainIndex = path[2];
            data = account.concat(chainIndex);
            p1 = (internalChain ? BTChip.INTERNAL_CHAIN : BTChip.EXTERNAL_CHAIN);
        }
        else {
            data = new ByteString(Convert.toHexByte(path.length), HEX);
            for (var i = 0; i < path.length; i++) {
                data = data.concat(path[i]);
            }
            p1 = 0x00;
        }
        return this.card.sendApdu_async(0xe0, 0x40, p1, 0x00, data, [0x9000]).then(function (result) {
            var resultList = {};
            var offset = 0;
            resultList['publicKey'] = result.bytes(offset + 1, result.byteAt(offset));
            offset += result.byteAt(offset) + 1;
            resultList['bitcoinAddress'] = result.bytes(offset + 1, result.byteAt(offset));
            /* AJOUT NESS */
            offset += result.byteAt(offset) + 1;
            resultList['chainCode'] = result.bytes(offset, 32);
            /* FIN AJOUT NESS */
            return resultList;
        });
    },
   */

  def derivePublicAddress(path: DerivationPath, networkParameters: NetworkParameters)
    : Future[PublicAddressResult] =
    firmwareVersion() flatMap {(version) =>
      if (version.usesDeprecatedBip32Derivation) {
        throw LedgerUnsupportedFirmwareException()
      }
      val writer = new BytesWriter(path.length * 4 + 1)
      writer.writeByte(path.length)
      for (i <- 0 to path.depth) {
        val n = path(i).get
        writer.writeInt(n.childNum)
      }
      $$(s"GET PUBLIC ADDRESS $path") {
        sendApdu(0xe0, 0x40, 0x00, 0x00, writer.toByteArray, 0x00) map {(result) =>
          matchErrorsAndThrow(result)
          PublicAddressResult(result.data)
        }
      }
    }

  def deriveExtendedPublicKey(path: DerivationPath, network: NetworkParameters): Future[DeterministicKey] = {

    def finalize(fingerprint: Long): Future[DeterministicKey] = {
      derivePublicAddress(path, network) map {(result) =>
        val magic = network.getBip32HeaderPub
        val depth = path.length.toByte
        val childNum = path.childNum
        val chainCode = result.chainCode
        val publicKey = Crypto.compressPublicKey(result.publicKey)
        val rawXpub = new BytesWriter(13 + chainCode.length + publicKey.length)
        rawXpub.writeInt(magic)
        rawXpub.writeByte(depth)
        rawXpub.writeInt(fingerprint)
        rawXpub.writeInt(childNum)
        rawXpub.writeByteArray(chainCode)
        rawXpub.writeByteArray(publicKey)
        val xpub58 = Base58.encodeWitchChecksum(rawXpub.toByteArray)
        DeterministicKey.deserializeB58(xpub58, network)
      }
    }

    if (path.depth > 0) {
      derivePublicAddress(path.parent, network) flatMap {(result) =>
        val hash160 = Hash160.hash(Crypto.compressPublicKey(result.publicKey))
        val fingerprint: Long =
            ((hash160(0) & 0xFFL) << 24) |
            ((hash160(1) & 0xFFL) << 16) |
            ((hash160(2) & 0xFFL) << 8) |
            (hash160(3) & 0xFFL)
        finalize(fingerprint)
      }
    } else {
      finalize(0)
    }
  }

  /*
  _initialize: (callback, legacyMode) ->
    derivationPath = @_derivationPath.substring(0, @_derivationPath.length - 1)
    path = derivationPath.split '/'
    bitcoin = new BitcoinExternal()
    finalize = (fingerprint) =>
      @_wallet.getPublicAddress derivationPath, (nodeData, error) =>
        return callback?(null, error) if error?
        publicKey = bitcoin.compressPublicKey nodeData.publicKey
        depth = path.length
        lastChild = path[path.length - 1].split('\'')
        if legacyMode
          childnum = (0x80000000 | parseInt(lastChild)) >>> 0
        else if lastChild.length is 1
          childnum = parseInt(lastChild[0])
        else
          childnum = (0x80000000 | parseInt(lastChild[0])) >>> 0
        @_xpub = @_createXPUB depth, fingerprint, childnum, nodeData.chainCode, publicKey, ledger.config.network.name
        @_xpub58 = @_encodeBase58Check @_xpub
        @_hdnode = GlobalContext.bitcoin.HDNode.fromBase58 @_xpub58
        callback?(@)

    if path.length > 1
      prevPath = path.slice(0, -1).join '/'
      @_wallet.getPublicAddress prevPath, (nodeData, error) =>
        return callback?(null, error) if error?
        publicKey = bitcoin.compressPublicKey nodeData.publicKey
        ripemd160 = new JSUCrypt.hash.RIPEMD160()
        sha256 = new JSUCrypt.hash.SHA256();
        result = sha256.finalize(publicKey.toString(HEX));
        result = new ByteString(JSUCrypt.utils.byteArrayToHexStr(result), HEX)
        result = ripemd160.finalize(result.toString(HEX))
        fingerprint = ((result[0] << 24) | (result[1] << 16) | (result[2] << 8) | result[3]) >>> 0
        finalize fingerprint
    else
      finalize 0

  _createXPUB: (depth, fingerprint, childnum, chainCode, publicKey, network) ->
    magic = if ledger?.config?.network? then  Convert.toHexInt(ledger.config.network.bitcoinjs.bip32.public) else "0488B21E"
    xpub = new ByteString magic, HEX
    xpub = xpub.concat new ByteString(_.str.lpad(depth.toString(16), 2, '0'), HEX)
    xpub = xpub.concat new ByteString(_.str.lpad(fingerprint.toString(16), 8, '0'), HEX)
    xpub = xpub.concat new ByteString(_.str.lpad(childnum.toString(16), 8, '0'), HEX)
    xpub = xpub.concat new ByteString(chainCode.toString(HEX), HEX)
    xpub = xpub.concat new ByteString(publicKey.toString(HEX), HEX)
    xpub
   */

}
object LedgerDerivationApi {

  case class PublicAddressResult(publicKey: Array[Byte],
                                 address: String,
                                 chainCode: Array[Byte]
                                  ) {

  }

  object PublicAddressResult {

    def apply(reader: BytesReader): PublicAddressResult = {
      var length = reader.readNextByte() & 0xFF
      val publicKey = reader.readNextBytes(length)
      length = reader.readNextByte() & 0xFF
      val address = AsciiUtils.toString(reader.readNextBytes(length))
      val chainCode = reader.readNextBytesUntilEnd()
      new PublicAddressResult(publicKey, address, chainCode)
    }

  }

}