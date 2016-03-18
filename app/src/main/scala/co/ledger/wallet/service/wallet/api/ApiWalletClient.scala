/**
  *
  * ApiWallet
  * Ledger wallet
  *
  * Created by Pierre Pollastri on 17/03/16.
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
package co.ledger.wallet.service.wallet.api

import android.content.Context
import co.ledger.wallet.core.utils.logs.Loggable
import co.ledger.wallet.service.wallet.AbstractDatabaseStoredWallet
import co.ledger.wallet.wallet.{Account, ExtendedPublicKeyProvider}
import de.greenrobot.event.EventBus
import org.bitcoinj.core.{Coin, Transaction, NetworkParameters}

import scala.concurrent.Future

class ApiWalletClient(context: Context, name: String, networkParameters: NetworkParameters)
  extends AbstractDatabaseStoredWallet(context, name, networkParameters) with Loggable {

  override def account(index: Int): Future[Account] = ???

  override def synchronize(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = ???

  override def accounts(): Future[Array[Account]] = ???

  override def setup(publicKeyProvider: ExtendedPublicKeyProvider): Future[Unit] = ???

  override def isSynchronizing(): Future[Boolean] = ???

  override def balance(): Future[Coin] = ???

  override def accountsCount(): Future[Int] = ???

  override def eventBus: EventBus = ???

  override def pushTransaction(transaction: Transaction): Future[Unit] = ???

  override def needsSetup(): Future[Boolean] = ???

  private var _accounts = Array[ApiAccountClient]()
}
