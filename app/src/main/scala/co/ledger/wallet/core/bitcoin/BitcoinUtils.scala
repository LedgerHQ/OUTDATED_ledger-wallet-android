/**
 *
 * BitcoinUtils
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/02/15.
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
package co.ledger.wallet.core.bitcoin

import co.ledger.wallet.wallet.Utxo
import org.bitcoinj.core.{Coin, NetworkParameters}
import org.bitcoinj.params.Networks

object BitcoinUtils {

  def isAddressValid(address: String): Boolean = Base58.verify(address)

  def getNetworkFromCoinVersions(regularCoinVersion: Int, p2shCoinVersion: Int)
  : Option[NetworkParameters] = {
    val iterator = Networks.get().iterator()
    while (iterator.hasNext) {
      val network = iterator.next()
      if (network.getAddressHeader == regularCoinVersion && network.getP2SHHeader == p2shCoinVersion)
        return Option(network)
    }
    None
  }

  /** *
    * Estimates the transaction size in bytes once it will be signed
    * @param utxo The list of inputs
    * @param outputsCount The number of outputs
    * @return The estimated size of the signed transaction (plus or minus the number of inputs)
    */
  def estimateTransactionSize(utxo: Array[Utxo], outputsCount: Int): Range = {
    val median: Int = 148 * utxo.length + outputsCount * 34 + 10
    (median - utxo.length) to (median + utxo.length)
  }



}
