package co.ledger.wallet

import android.net.Uri

/**
 *
 * BaseConfig
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 17/11/15.
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
trait BaseConfig {

  def ApiBaseUri = Uri.parse("https://api.ledgerwallet.com")
  def WebSocketBaseUri = Uri.parse("wss://ws.ledgerwallet.com")
  def WebSocketChannelsUri = WebSocketBaseUri.buildUpon().appendEncodedPath("2fa/channels").build()
  @deprecated
  def LedgerAttestationPublicKey =
    "04c370d4013107a98dfef01d6db5bb3419deb9299535f0be47f05939a78b314a3c29b51fcaa9b3d46fa382c995456af50cd57fb017c0ce05e4a31864a79b8fbfd6" //"04e69fd3c044865200e66f124b5ea237c918503931bee070edfcab79a00a25d6b5a09afbee902b4b763ecf1f9c25f82d6b0cf72bce3faf98523a1066948f1a395f"
  def HelpCenterUri = Uri.parse("http://support.ledgerwallet.com/help_center")
  def SupportEmailAddress = "hello@ledger.fr"
  def Env = if (BuildConfig.DEBUG) "dev" else "prod"
  def TrustletWebPage = Uri.parse("https://www.ledgerwallet.com/beta/trustlet")
  def DisableLogging = !BuildConfig.DEBUG

  def GreenBitsPackageName = "com.greenaddress.greenbits_android_wallet"
  def MyceliumPackageName = "com.mycelium.wallet"

  def CheckpointFilePath = "checkpoints"

  def LedgerAttestationsPublicKeys = Map(
    (0, 1) -> "04e69fd3c044865200e66f124b5ea237c918503931bee070edfcab79a00a25d6b5a09afbee902b4b763ecf1f9c25f82d6b0cf72bce3faf98523a1066948f1a395f",
    (1, 1) -> "04223314cdffec8740150afe46db3575fae840362b137316c0d222a071607d61b2fd40abb2652a7fea20e3bb3e64dc6d495d59823d143c53c4fe4059c5ff16e406",
    (2, 1) ->
      "04c370d4013107a98dfef01d6db5bb3419deb9299535f0be47f05939a78b314a3c29b51fcaa9b3d46fa382c995456af50cd57fb017c0ce05e4a31864a79b8fbfd6"
    //(2, 1) ->
     //"04e69fd3c044865200e66f124b5ea237c918503931bee070edfcab79a00a25d6b5a09afbee902b4b763ecf1f9c25f82d6b0cf72bce3faf98523a1066948f1a395f"
    //(3, 1) -> "ADDKEYHERE"
  )

}
