/**
 *
 * WalletService
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 24/11/15.
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
package co.ledger.wallet.service.wallet

import java.io.File
import java.util.concurrent.ConcurrentHashMap

import android.app.Service
import android.content.{Context, Intent}
import android.os.{Handler, IBinder}
import co.ledger.wallet.app.tasks.CurrenciesValueRefreshTask
import co.ledger.wallet.app.wallet.WalletPreferences
import co.ledger.wallet.core.utils.Preferences
import co.ledger.wallet.core.utils.logs.{Loggable, Logger}
import co.ledger.wallet.preferences.WalletPreferencesProtos
import co.ledger.wallet.service.wallet.api.ApiWalletClient
import co.ledger.wallet.service.wallet.spv.SpvWalletClient
import co.ledger.wallet.wallet.{DerivationPath, ExtendedPublicKeyProvider, Wallet}
import org.bitcoinj.core.NetworkParameters
import org.bitcoinj.crypto.DeterministicKey
import org.bitcoinj.params.MainNetParams

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.{Failure, Success}

class WalletService extends Service with Loggable {
  import WalletService._
  import scala.concurrent.duration._

  implicit val ec = co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui

  val ShutdownDelay = 15 minutes
  val NumberOfPregeneratedXpubs = 5

  def wallet(name: String, engine: Int): Wallet = {
    _currentWallet getOrElse {
      val wallet: Wallet = engine match {
        case WalletService.WalletSpvEngine =>
          new SpvWalletClient(this, s"wallet_$name", xpubProviderProxy, networkParams)
        case WalletService.WalletApiEngine =>
          new ApiWalletClient(this, s"wallet_$name", xpubProviderProxy, networkParams)
      }
      synchronized(_currentWallet = Some(wallet))
      wallet
    }
  }

  def networkParams = {
    // TODO: Proper implementation
    _currentWallet map {(wallet) =>
      // Get coin from preferences
      MainNetParams.get()
    } getOrElse {
      MainNetParams.get()
    }
  }

  def openWallet(name: String): Wallet = {
    val prefs = preferences(name)
    WalletService.setCurrentWalletName(name)(this)
    wallet(name, prefs.synchronizer)
  }

  def defaultEngineFlag = WalletService.WalletApiEngine
  def currentWalletName: Option[String] =
    Option(_preferences.reader.getString(CurrentWalletNameKey, null))

  def currentWallet: Option[Wallet] = _currentWallet match {
    case Some(wallet) => _currentWallet
    case None =>
      _currentWallet = currentWalletName map openWallet
      _currentWallet
  }

  def closeCurrentWallet(): Unit = currentWallet foreach {(wallet) =>
    WalletService.closeCurrentWallet()(this)
    // TODO: Close
  }

  override def onCreate(): Unit = {
    super.onCreate()
    this.startService(new Intent(this, this.getClass))
    currencyRefresher.start()
  }

  def notifyActivityPaused(): Unit = {
    // Start shutdown timer
    _currentWallet match {
      case Some(_) => _handler.postDelayed(_shutDownRunnable, ShutdownDelay.toMillis)
      case None => _handler.postDelayed(_shutDownRunnable, 0)
    }
  }

  def notifyActivityResumed(): Unit = {
    // Cancel scheduled shutdown
    _handler.removeCallbacks(_shutDownRunnable)
  }

  override def onBind(intent: Intent): IBinder = _binder

  class Binder extends android.os.Binder {
    def service = WalletService.this
  }

  private[this] val _binder = new Binder
  private[this] var _currentWallet: Option[Wallet] = None
  private[this] val _walletsPreferences = new ConcurrentHashMap[String, WalletPreferences]().asScala
  private[this] lazy val _preferences = Preferences(WalletServicePreferencesName)(this)
  private[this] val _handler = new Handler()
  private[this] val _shutDownRunnable = new Runnable {
    override def run(): Unit = synchronized {
      _currentWallet foreach {wallet =>
        wallet.stop()
      }
      _currentWallet = None
      currencyRefresher.stop()
      stopSelf()
    }
  }

  // Wallet Preferences

  def preferences(walletName: String): WalletPreferences = {
    _walletsPreferences.lift(walletName).getOrElse {
      val directory = new File(getFilesDir, s"wallet_$walletName")
      val preferences = new WalletPreferences(directory)
      _walletsPreferences(walletName) = preferences
      preferences
    }
  }

  def xpubProvider_=(provider: ExtendedPublicKeyProvider): Unit = {
    xpubProviderProxy.provider = provider
  }

  def xpubProvider = xpubProviderProxy.provider

  def isXpubRequired = xpubProviderProxy.requiredXpub

  private val xpubProviderProxy = new XpubProviderProxy()

  private class XpubProviderProxy extends ExtendedPublicKeyProvider {

    implicit val ec = co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui

    override def generateXpub(path: DerivationPath, networkParameters: NetworkParameters):
    Future[DeterministicKey] = Future {
      // The proxy will always try to keep more xpubs than needed
      // in order to prevent the user to connect its device to often
      _provider.orNull
    } flatMap {(provider) =>
      val prefs = preferences(currentWalletName.get)
      def deriveAndCache(path: DerivationPath): Future[DeterministicKey] = {
        if (prefs.xpubs.contains(path.toString)) {
          Future.successful(DeterministicKey.deserializeB58(prefs.xpubs(path.toString), networkParams))
        } else if (provider != null) {
          Logger.d(s"Derivating xpub from $path")
          provider.generateXpub(path, networkParameters) andThen {
            case Success(key) =>
              requiredXpub = false
              prefs.xpubs(path.toString) = key.serializePubB58(networkParams)
              prefs.save()
            case Failure(ex) =>
              requiredXpub = false
          }
        } else {
          Future.failed(new Exception("Extended public provider available"))
        }
      }
      def deriveUntilEnough(): Unit = {
        val maxPath = new DerivationPath(path.parent, path.childNum + NumberOfPregeneratedXpubs)
        if (!prefs.xpubs.contains(maxPath.toString)) {
          Logger.d(s"Feed cache from ${path.toString} to ${maxPath.toString}")
          def iterate(currentChildNum: Long): Future[DeterministicKey] = {
            val p = new DerivationPath(path.parent, currentChildNum)
            Logger.d(s"Feed ${p.toString}")
            deriveAndCache(p) flatMap {(k) =>
              if (currentChildNum + 1 > maxPath.childNum)
                Future.successful(k)
              else
                iterate(currentChildNum + 1)
            }
          }
          iterate(path.childNum + 1) recover {
            case error: Throwable =>
              error.printStackTrace()
              throw error
          }
        }
      }
      if (prefs.xpubs.contains(path.toString)) {
        deriveUntilEnough()
        Future.successful(DeterministicKey.deserializeB58(prefs.xpubs(path.toString), networkParams))
      } else {
        requiredXpub = true
        deriveAndCache(path) andThen {
          case Success(_) =>
            deriveUntilEnough()
          case Failure(_) => // Do nothing
        }
      }
    }

    var requiredXpub = false

    def provider_=(extendedPublicKeyProvider: ExtendedPublicKeyProvider): Unit = Future {
      _provider = Option(extendedPublicKeyProvider)
    }
    def provider = _provider
    private var _provider: Option[ExtendedPublicKeyProvider] = None
  }


  // Currencies
  lazy val currencyRefresher = new CurrenciesValueRefreshTask(this)
}

object WalletService {

  val WalletServicePreferencesName = "WalletServicePreferences"
  val CurrentWalletNameKey = "current_wallet_name"

  val WalletSpvEngine = WalletPreferencesProtos.WalletPreferences.SPV
  val WalletApiEngine = WalletPreferencesProtos.WalletPreferences.API

  def currentWalletName(implicit context: Context): Option[String] = {
    val preferences = Preferences(WalletServicePreferencesName)(context)
    Option(preferences.reader.getString(CurrentWalletNameKey, null))
  }

  def setCurrentWalletName(walletIdentifier: String)(implicit context: Context): Unit = {
    val preferences = Preferences(WalletServicePreferencesName)(context)
    preferences.writer.putString(CurrentWalletNameKey, walletIdentifier).apply()
  }

  def closeCurrentWallet()(implicit context: Context): Unit = {
    val preferences = Preferences(WalletServicePreferencesName)(context)
    preferences.writer.remove(CurrentWalletNameKey).apply()
  }

}