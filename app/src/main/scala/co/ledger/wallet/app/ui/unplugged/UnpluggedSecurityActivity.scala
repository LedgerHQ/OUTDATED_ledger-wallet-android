/**
 *
 * UnpluggedSecurityActivity
 * Ledger wallet
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
package co.ledger.wallet.app.ui.unplugged

import android.os.Bundle
import android.text.SpannableString
import android.text.style.UnderlineSpan
import co.ledger.wallet.R
import co.ledger.wallet.common._
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.widget.TextView

class UnpluggedSecurityActivity extends UnpluggedSetupActivity {

  lazy val textView = TR(R.id.textViewLine2).as[TextView]
  lazy val button = TR(R.id.button).as[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_security_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    stepNumberTextView.setText(R.string.unplugged_security_step_number)
    stepInstructionTextView.setText(R.string.unplugged_security_step_text)
    if (isInCreationMode) {
      textView.setText(R.string.unplugged_security_line_2_creation)
    }
    else {
      textView.setText(R.string.unplugged_security_line_2_restore)
    }

    button onClick {
      startNextActivity(classOf[UnpluggedPINChoiceActivity])
    }

    val text = TR(R.id.textViewLine3).as[TextView]
    text.underlined = true
  }

}