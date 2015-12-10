/**
 *
 * SpvSynchronizationHelper
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

import java.io.{InputStream, File}
import java.util.Date

import co.ledger.wallet.service.wallet.database.WalletDatabaseOpenHelper
import org.bitcoinj.core.{Wallet => JWallet, _}
import org.bitcoinj.net.discovery.{PeerDiscovery, DnsDiscovery}

import scala.concurrent.{Promise, Future}

class SpvSynchronizationHelper(
                                networkParameters: NetworkParameters,
                                directory: File,
                                database: WalletDatabaseOpenHelper) {

  def setup(wallets: Array[JWallet], fastCatchupDate: Date, checkpoints: InputStream): Future[SpvAppKit] = {
    val promise = Promise[SpvAppKit]()

    promise.future
  }

  def synchronize(appKit: SpvAppKit): Future[Unit] = {
    null
  }

  def loadWalletsFromDatabase(): Future[Array[JWallet]] = {
    null
  }

  def startPeerGroup(wallets: Array[JWallet]): Future[SpvAppKit] = {
    null
  }

  def discovery = _discovery
  def discovery_=(discovery: PeerDiscovery): Unit = {
    require(discovery != null)
    _discovery = discovery
  }
  private[this] var _discovery: PeerDiscovery = new DnsDiscovery(networkParameters)

}
