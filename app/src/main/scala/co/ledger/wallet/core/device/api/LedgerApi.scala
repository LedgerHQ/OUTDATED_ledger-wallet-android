/**
 *
 * LedgerApi
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

import java.util.UUID

import co.ledger.wallet.core.device.Device
import co.ledger.wallet.wallet.DerivationPath
import co.ledger.wallet.wallet.DerivationPath.Root
import org.bitcoinj.core.NetworkParameters

import scala.concurrent.{Future, ExecutionContext}

class LedgerApi(override val device: Device)
  extends LedgerCommonApiInterface
  with LedgerFirmwareApi
  with LedgerDerivationApi
  with LedgerLifecycleApi {
  override implicit val ec: ExecutionContext = co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.main


  def walletIdentifier(network: NetworkParameters): Future[String] = {
    derivePublicAddress(new DerivationPath(Root, 0), network).map {(result) =>
      result.address
    }
  }

  val uuid = UUID.randomUUID()
}

object LedgerApi {

  def apply(device: Device): LedgerApi = {
    val lastApi = _lastApi.filter(device.uuid == _.device.uuid)
    lastApi.getOrElse {
      val api = new LedgerApi(device)
      _lastApi = Some(api)
      api
    }
  }

  def apply(uuid: UUID): Option[LedgerApi] = _lastApi filter(_.uuid == uuid)

  private var _lastApi: Option[LedgerApi] = None

}

