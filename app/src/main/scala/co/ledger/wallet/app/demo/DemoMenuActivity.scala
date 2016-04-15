/**
 *
 * DemoMenuActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 22/02/16.
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
package co.ledger.wallet.app.demo

import android.app.Activity
import android.content.Intent
import android.support.v7.app.AlertDialog
import android.view.{MenuItem, Menu}
import co.ledger.wallet.R
import co.ledger.wallet.app.SettingsActivity
import co.ledger.wallet.core.base.BaseActivity
import co.ledger.wallet.core.utils.AndroidUtils
import co.ledger.wallet.core.utils.logs.LogCatReader
import co.ledger.wallet.legacy.HomeActivity
import co.ledger.wallet.legacy.unplugged.UnpluggedTapActivity

import scala.util.{Failure, Success}

trait DemoMenuActivity extends BaseActivity {

  override def onCreateOptionsMenu(menu: Menu): Boolean = {
    getMenuInflater.inflate(R.menu.demo_activity_menu, menu)
    if (!AndroidUtils.hasNfcFeature()) {
      menu.findItem(R.id.setup_unplugged).setVisible(false)
    }
    menu.findItem(R.id.settings).setVisible(false)
    true
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)
    item.getItemId match {
      case R.id.export_logs =>
        exportLogs()
        true
      case R.id.settings =>
        startSettingsActivity()
        true
      case R.id.setup_unplugged =>
        startConfigureUnplugged()
        true
      case R.id.m2fa =>
        startM2FA()
        true
      case R.id.reconnect =>
        onClickOnReconnectLastDevice()
        true
      case somethingElse => false
    }
  }

  def onClickOnReconnectLastDevice(): Unit = {
    new AlertDialog.Builder(this).setTitle("Error").setMessage("This feature is not available " +
      "here").show()
  }

  private[this] def exportLogs(): Unit = {
    LogCatReader.createEmailIntent(this).onComplete {
      case Success(intent) =>
        startActivity(intent)
      case Failure(ex) =>
        ex.printStackTrace()
    }
  }

  private[this] def startSettingsActivity(): Unit = startActivity(new Intent(this, classOf[SettingsActivity]))

  def startConfigureUnplugged(): Unit =
    startActivity(new Intent(this, classOf[UnpluggedTapActivity]).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

  def startM2FA(): Unit =
    startActivity(new Intent(this, classOf[HomeActivity]).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK))

}
