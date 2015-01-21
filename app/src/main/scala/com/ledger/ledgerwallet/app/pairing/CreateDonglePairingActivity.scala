/**
 *
 * CreateDonglePairingActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/01/15.
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
package com.ledger.ledgerwallet.app.pairing

import android.os.Bundle
import android.support.v4.app.NavUtils
import android.view.MenuItem
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.{BaseFragment, BaseActivity}
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.TextView

class CreateDonglePairingActivity extends BaseActivity with CreateDonglePairingActivity.CreateDonglePairingProccessContract {

  lazy val stepNumberTextView = TR(R.id.step_number).as[TextView]
  lazy val stepInstructionTextView = TR(R.id.instruction_text).as[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.create_dongle_pairing_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    gotToStep(1, TR(R.string.create_dongle_instruction_step_1).as[String], new ScanPairingQrCodeFragment())
  }

  override def gotToStep(stepNumber: Int, instructionText: CharSequence, fragment: BaseFragment): Unit = {
    stepNumberTextView.setText(stepNumber.toString + ".")
    stepInstructionTextView.setText(instructionText)
    val ft = getSupportFragmentManager.beginTransaction()
    if (stepNumber > 1)
      ft.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.slide_from_left, R.anim.slide_to_right)
    ft.replace(R.id.fragment_container, fragment)
    ft.commit()
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    item.getItemId match {
      case android.R.id.home => {
        NavUtils.navigateUpFromSameTask(this)
        true
      }
      case _ => super.onOptionsItemSelected(item)
    }
  }
}

object CreateDonglePairingActivity {

  trait CreateDonglePairingProccessContract {
    def gotToStep(stepNumber: Int, instructionText: CharSequence, fragment: BaseFragment): Unit
  }

}
