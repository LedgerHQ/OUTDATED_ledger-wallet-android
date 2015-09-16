/**
 *
 * UnpluggedTapActivity
 * ledger-wallet-android
 *
 * Created by Nicolas Bigot on 15/09/15.
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

package com.ledger.ledgerwallet.app.unplugged

import android.os.Bundle
import android.widget.Toast
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.nfc.Unplugged
import com.ledger.ledgerwallet.utils.logs.Logger

import scala.util.{Failure, Success}

class UnpluggedTapActivity extends UnpluggedSetupActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_tap_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
  }

  override protected def onUnpluggedDiscovered(unplugged: Unplugged): Unit = {
    super.onUnpluggedDiscovered(unplugged)
    unplugged.checkIsSetup() onComplete {
      case Success(isSetup) =>
        if (isSetup) {
          startNextActivity(classOf[UnpluggedExistingActivity])
        } else {
          startNextActivity(classOf[UnpluggedWelcomeActivity])
        }
      case Failure(error) =>
        error.printStackTrace()
        Toast.makeText(this, R.string.unplugged_tap_error_occured, Toast.LENGTH_LONG).show()
    }
  }


  override protected def onNotInstalledTagDiscovered(): Unit = {
    startNextActivity(classOf[])
  }

  override protected def onDiscoveredTagError(error: Throwable): Unit = {
    super.onDiscoveredTagError(error)
    Toast.makeText(this, R.string.unplugged_tap_error_occured, Toast.LENGTH_LONG).show()
  }
}