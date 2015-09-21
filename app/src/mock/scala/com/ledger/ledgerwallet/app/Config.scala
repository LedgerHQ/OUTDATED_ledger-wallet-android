package co.ledger.wallet.app

import android.net.Uri
import com.ledger.ledgerwallet.BuildConfig

/**
 *
 * Config
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/06/15.
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

object Config {

  def ApiBaseUri = Uri.parse("https://api.ledgerwallet.com")
  def WebSocketBaseUri = Uri.parse("wss://ws.ledgerwallet.com")
  def LedgerAttestationPublicKey = "0478c0837ded209265ea8131283585f71c5bddf7ffafe04ccddb8fe10b3edc7833d6dee70c3b9040e1a1a01c5cc04fcbf9b4de612e688d09245ef5f9135413cc1d"
  def HelpCenterUri = Uri.parse("http://support.ledgerwallet.com/help_center")
  def SupportEmailAddress = "hello@ledger.fr"
  def Env = if (BuildConfig.DEBUG) "dev" else "prod"
  def TrustletWebPage = Uri.parse("https://ledgerwallet.com/trustlet")
  def DisableLogging = !BuildConfig.DEBUG
}
