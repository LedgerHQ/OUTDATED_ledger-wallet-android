/**
 *
 * SpvKit
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/12/15.
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
package co.ledger.wallet.service.wallet.spv

import co.ledger.wallet.service.wallet.database.model.AccountRow
import org.bitcoinj.core.{Wallet => JWallet, PeerGroup, BlockChain}
import org.bitcoinj.store.BlockStore

class SpvAppKit(
  val blockStore: BlockStore,
  val blockChain: BlockChain,
  val peerGroup: PeerGroup,
  val accounts: Array[(AccountRow, JWallet)]) {

  def close(): Unit = {

  }

}

class SpvAppKitBuilder(private val blockStore: BlockStore = null,
                       private val blockChain: BlockChain = null,
                       private val peerGroup: PeerGroup = null,
                       private val accounts: Array[(AccountRow, JWallet)] = Array()) {

  def copy(blockStore: BlockStore = blockStore,
           blockChain: BlockChain = blockChain,
           peerGroup: PeerGroup = peerGroup,
           accounts: Array[(AccountRow, JWallet)] = accounts): SpvAppKitBuilder = {
    new SpvAppKitBuilder(blockStore, blockChain, peerGroup, accounts)
  }

  def withBlockStore(blockStore: BlockStore) = copy(blockStore = blockStore)
  def withBlockChain(blockChain: BlockChain) = copy(blockChain = blockChain)
  def withPeerGroup(peerGroup: PeerGroup) = copy(peerGroup = peerGroup)
  def addAccount(accountRow: AccountRow, wallet: JWallet) = {
    copy(accounts = (accounts :+ (accountRow, wallet)).sortBy(_._1.index))
  }

  def build() = new SpvAppKit(blockStore, blockChain, peerGroup, accounts)

}