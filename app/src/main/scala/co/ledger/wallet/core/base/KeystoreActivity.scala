/**
 *
 * KeystoreActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/10/15.
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
package co.ledger.wallet.core.base

import android.app.Activity
import co.ledger.wallet.core.security.Keystore
import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui
import scala.util.{Failure, Success}

trait KeystoreActivity extends Activity {


  override abstract def onResume(): Unit = {
    super.onResume()
    if (_keystore == null || _keystore == Keystore.defaultInstance(this)) {
      _keystore = Keystore.defaultInstance(this)
      _keystore.load(null) onComplete {
        case Success(_) =>
          onKeystoreInstanceReady(_keystore)
          super.onResume()
        case Failure(ex) =>
          _keystore.promptUnlockDialog(this) onComplete {
            case Success(_) =>
              onKeystoreInstanceReady(_keystore)
            case Failure(err) => finish()
          }
      }

    } else {
      onKeystoreInstanceReady(_keystore)
    }

  }

  protected def onKeystoreInstanceReady(newKeystore: Keystore): Unit = {

  }

  def keystore = _keystore
  private[this] var _keystore: Keystore = null
}



