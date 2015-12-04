/**
 *
 * SpvAccountClient
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 25/11/15.
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

import java.io.File
import java.util
import java.util.Date
import java.util.concurrent.{ConcurrentHashMap, TimeUnit}
import java.util.concurrent.atomic.AtomicReference

import android.content.Context
import co.ledger.wallet.core.concurrent.ThreadPoolTask
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.wallet.events.PeerGroupEvents.{StartSynchronization,
SynchronizationProgress}
import co.ledger.wallet.wallet.events.WalletEvents.{CoinReceived, CoinSent, AccountUpdated,
AccountCreated}
import co.ledger.wallet.wallet.exceptions.AccountHasNoXpubException
import co.ledger.wallet.wallet.{ExtendedPublicKeyProvider, Wallet, Account}
import org.bitcoinj.core._
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.script.Script
import org.json.JSONObject
import scala.collection.JavaConverters._
import scala.concurrent.{Promise, Future}
import scala.util.{Try, Failure, Success}

class SpvAccountClient(val wallet: SpvWalletClient, val index: Int)
  extends Account with Loggable {

  implicit val ec = wallet.ec // Work on the wallet queue

  val XpubKey = "xpub"

  type JWallet = org.bitcoinj.core.Wallet
  type JSpvBlockChain = org.bitcoinj.core.BlockChain
  type JPeerGroup = PeerGroup

  override def synchronize(provider: ExtendedPublicKeyProvider): Future[Unit] =
    wallet.synchronize(provider)

  override def xpub(): Future[DeterministicKey] = Future {
    _xpub.isDefined
  } flatMap {(hasXpub) =>
    if (hasXpub)
      Future.successful(_xpub.get)
    else
      load() map {(_) =>
        _xpub.get
      }
  }

  def xpub(key: DeterministicKey): Future[Unit] = {
    _xpub = Some(key)
    load().flatMap({(_) =>
      _persistentState.put(XpubKey, key.serializePubB58(wallet.networkParameters))
      wallet.save()
    })
  }

  def hasXpub = _xpub.isDefined

  override def transactions(): Future[Set[Transaction]] =
    load().map(_.getTransactions(false).asScala.toSet)

  override def balance(): Future[Coin] = load().map({(wallet) => wallet.getBalance})

  override def freshPublicAddress(): Future[Address] = load().map({(wallet) => wallet.freshReceiveAddress()})

  def load(): Future[JWallet] = Future.successful() flatMap { (_) =>
    if (_walletFuture == null) {
      // If no xpub fail
      _walletFuture = wallet.accountPersistedJson(index) flatMap {(json) =>
        // We could use a WalletExtension instead of using our own file for storing metadata but
        // no ;). We need to store accounts information in the SpvWalletClient so instead of
        // saving data on multiple location, we save our metadata into the same json for every
        // accounts
        _persistentState = json
        Logger.d(s"Init account $index ${_persistentState.toString}")
        if (_xpub.isEmpty && !json.has(XpubKey)) {
          _walletFuture = null
          throw new AccountHasNoXpubException(index)
        } else if (_xpub.isEmpty) {
          _xpub = Some(DeterministicKey.deserializeB58(json.getString(XpubKey), wallet.networkParameters))
        }
        wallet.peerGroup().map({(peerGroup) =>
          var w: JWallet = null
          if (_walletFile.exists())
            w = org.bitcoinj.core.Wallet.loadFromFile(_walletFile)
          else {
            w = org.bitcoinj.core.Wallet.fromWatchingKey(wallet.networkParameters, _xpub.get)
          }
          wallet.blockChain.addWallet(w)
          w.autosaveToFile(_walletFile, 500L, TimeUnit.MILLISECONDS, null)
          peerGroup.addWallet(w)
          w.addEventListener(_walletEventListener)
          w
        })
      }
    }
    _walletFuture
  }

  private val _walletEventListener = new AbstractWalletEventListener {

    override def onCoinsReceived(w: JWallet, tx: Transaction, prevBalance: Coin, newBalance: Coin): Unit = {
      super.onCoinsReceived(w, tx, prevBalance, newBalance)
      wallet.eventBus.post(CoinReceived(index, prevBalance.subtract(newBalance)))
    }


    override def onCoinsSent(w: JWallet, tx: Transaction, prevBalance: Coin, newBalance:
    Coin): Unit = {
      super.onCoinsSent(w, tx, prevBalance, newBalance)
      wallet.eventBus.post(CoinSent(index, prevBalance.subtract(newBalance)))
    }

    override def onWalletChanged(w: JWallet): Unit = {
      super.onWalletChanged(w)
      wallet.eventBus.post(AccountUpdated(index))
    }
  }

  private var _walletFuture: Future[JWallet] = null
  private var _persistentState: JSONObject = null
  private var _xpub: Option[DeterministicKey] = None
  private lazy val _walletFile = new File(
    wallet.context.getDir("spv_wallets", Context.MODE_PRIVATE),
    s"${wallet.name}_$index"
  )
}