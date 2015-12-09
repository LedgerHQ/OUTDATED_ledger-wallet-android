/**
 *
 * SpvWalletClient
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
package co.ledger.wallet.service.wallet.spv

import android.content.Context
import co.ledger.wallet.core.concurrent.{AsyncCursor, SerialQueueTask}
import co.ledger.wallet.core.utils.logs.Loggable
import co.ledger.wallet.service.wallet.database.WalletDatabaseOpenHelper
import co.ledger.wallet.wallet.{Operation, ExtendedPublicKeyProvider, Account, Wallet}
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Wallet => JWallet, _}
import co.ledger.wallet.common._

import scala.concurrent.Future

class SpvWalletClient(val context: Context, val name: String, val networkParameters: NetworkParameters)
  extends Wallet with SerialQueueTask with Loggable {

  implicit val DisableLogging = false

  override def account(index: Int): Future[Account] = init() map {(_) => _accounts(index)}

  override def operations(batchSize: Int): Future[AsyncCursor[Operation]] = ???

  override def synchronize(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = ???

  override def accounts(): Future[Array[Account]] = init() map {(_) =>
    _accounts.asInstanceOf[Array[Account]]
  }

  override def isSynchronizing(): Future[Boolean] = ???

  override def balance(): Future[Coin] = ???

  override def accountsCount(): Future[Int] = init() map {(_) => _accounts.length}

  val eventBus: EventBus = new EventBus()

  private def init(): Future[Unit] = Future.successful() flatMap {(_) =>
    if (_spvAppKit.isEmpty) {
      _spvSynchronizationHelper
      null
    } else {
     Future.successful()
    }
  }

  private[this] var _accounts = Array[SpvAccountClient]()
  private[this] var _spvAppKit: Option[SpvAppKit] = None
  private[this] lazy val _database = new WalletDatabaseOpenHelper(context, name)
  private[this] lazy val _spvSynchronizationHelper =
    new SpvSynchronizationHelper(
      networkParameters,
      context.getDir(s"spv}", Context.MODE_PRIVATE),
      _database
    )
}
