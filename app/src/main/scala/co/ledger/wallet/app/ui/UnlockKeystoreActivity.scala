/**
 *
 * UnlockKeystoreActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 08/02/16.
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
package co.ledger.wallet.app.ui

import java.security.KeyStore.PasswordProtection

import android.content.Intent
import android.os.Bundle
import android.view.inputmethod.EditorInfo
import android.view.{MenuItem, Menu, KeyEvent}
import android.widget.{Toast, TextView}
import android.widget.TextView.OnEditorActionListener
import co.ledger.wallet.R
import co.ledger.wallet.app.base.BaseActivity
import co.ledger.wallet.app.ui.unplugged.UnpluggedTapActivity
import co.ledger.wallet.core.security.Keystore
import co.ledger.wallet.core.utils.logs.LogCatReader
import co.ledger.wallet.core.utils.{AndroidUtils, TR}
import co.ledger.wallet.core.widget.EditText

import co.ledger.wallet.core.concurrent.ExecutionContext.Implicits.ui
import scala.util.{Failure, Success}

class UnlockKeystoreActivity extends BaseActivity {

  lazy val passwordEditText = TR(R.id.password).as[EditText]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unlock_keystore_activity)
    passwordEditText.setImeActionLabel(getString(R.string.action_done), KeyEvent.KEYCODE_ENTER)
    passwordEditText.setOnEditorActionListener(new OnEditorActionListener {
      override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = {
        val isEnterEvent = event != null && event.getKeyCode == KeyEvent.KEYCODE_ENTER
        val isEnterUpEvent = isEnterEvent && event.getAction == KeyEvent.ACTION_UP
        val isEnterDownEvent = isEnterEvent && event.getAction == KeyEvent.ACTION_DOWN
        if (actionId == EditorInfo.IME_ACTION_DONE || isEnterUpEvent) {
          unlockKeystore()
          true
        } else if (isEnterDownEvent) {
          true
        } else {
          false
        }
      }
    })
  }

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.home_activity_menu, menu)
    if (!AndroidUtils.hasNfcFeature()) {
      menu.findItem(R.id.setup_unplugged).setVisible(false)
    }
    menu.findItem(R.id.settings).setVisible(false)
    true
  }

  def startConfigureUnplugged(): Unit =
    startActivity(new Intent(this, classOf[UnpluggedTapActivity]).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))


  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)
    item.getItemId match {
      case R.id.export_logs =>
        exportLogs()
        true
      case somethingElse => false
    }
  }

  def unlockKeystore(): Unit = {
    val password = passwordEditText.getText.toString
    Keystore.defaultInstance.load(new PasswordProtection(password.toCharArray)) onComplete {
      case Success(_) => finish()
      case Failure(ex) =>
        ex.printStackTrace()
        Toast.makeText(this, R.string.unlock_keystore_wrong_password, Toast.LENGTH_SHORT).show()
    }
  }

  private[this] def exportLogs(): Unit = {
    LogCatReader.createEmailIntent(this).onComplete {
      case Success(intent) =>
        startActivity(intent)
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

}
