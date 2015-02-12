/**
 *
 * Config
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 27/01/15.
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
package com.ledger.ledgerwallet.app

import android.net.Uri
import com.ledger.ledgerwallet.BuildConfig

object Config extends Config{

  private var _impl: Config = new ConfigImpl

  def apply(): Config = _impl
  def setImplementation(impl: Config): Unit = _impl = impl

  def ApiBaseUri = _impl.ApiBaseUri
  def WebSocketBaseUri = _impl.WebSocketBaseUri
  def LedgerAttestationPublicKey = _impl.LedgerAttestationPublicKey
  def HelpCenterUri = _impl.HelpCenterUri
  def Env = _impl.Env

  private class ConfigImpl extends Config {
    def ApiBaseUri = Uri.parse("https://api02.ledgerwallet.com")
    def WebSocketBaseUri = Uri.parse("ws://ws02.ledgerwallet.comr")
    def LedgerAttestationPublicKey = "04e69fd3c044865200e66f124b5ea237c918503931bee070edfcab79a00a25d6b5a09afbee902b4b763ecf1f9c25f82d6b0cf72bce3faf98523a1066948f1a395f"
    def HelpCenterUri = Uri.parse("http://support.ledgerwallet.com/help_center")
    def Env = if (BuildConfig.DEBUG) "dev" else "prod"
  }



}

trait Config {

  def ApiBaseUri: Uri
  def WebSocketBaseUri: Uri
  def HelpCenterUri: Uri
  def LedgerAttestationPublicKey: String
  def Env: String

}