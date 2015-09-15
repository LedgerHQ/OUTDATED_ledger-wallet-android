/**
 *
 * HomeActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 10/02/15.
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

import android.content.Context
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.view.KeyEvent
import android.view.inputmethod.{InputMethodManager, EditorInfo}
import android.widget.Toast
import com.ledger.ledgerwallet.{common, R}
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.{PinTextView, TextView}
import common._

class UnpluggedPINChoiceActivity extends UnpluggedSetupActivity {

  lazy val pinTextView = TR(R.id.pin_view).as[PinTextView]
  lazy val bottomTextView = TR(R.id.bottom_text).as[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_pin_choice_fragment)

    if (hasPinSetup) {
      // Confirm mode
      setTitle(R.string.unplugged_pin_choice_title_confirm)
      stepNumberTextView.setText(R.string.unplugged_pin_choice_step_number_confirm)
      stepInstructionTextView.setText(R.string.unplugged_pin_choice_step_instruction_confirm)
      bottomTextView.setText(R.string.unplugged_pin_choice_step_bottom_confirm)
    } else {
      // Enter mode
      setTitle(R.string.unplugged_pin_choice_title_enter)
      stepNumberTextView.setText(R.string.unplugged_pin_choice_step_number_enter)
      stepInstructionTextView.setText(R.string.unplugged_pin_choice_step_instruction_enter)
      bottomTextView.setText(R.string.unplugged_pin_choice_step_bottom_enter)
    }

    pinTextView.requestFocus()
    pinTextView.setFocusableInTouchMode(true)
    pinTextView.setOnEditorActionListener((actionId: Int, event: KeyEvent) => {
      actionId match {
        case EditorInfo.IME_ACTION_NEXT =>
          navigateNext()
          true
        case _ => false
      }
    })

    pinTextView.addTextChangedListener(new TextWatcher {
      override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
      override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
      override def afterTextChanged(s: Editable): Unit = {
        if (s.length() == 4)
          navigateNext()
      }
    })
  }

  protected[this] def navigateNext(): Unit = {
    if (hasPinSetup) {
      if (pin.get != pinTextView.getEditableText.toString) {
        Toast.makeText(this, R.string.unplugged_pin_choice_dont_match_error, Toast.LENGTH_LONG).show()
      } else {
        val imm = getSystemService(Context.INPUT_METHOD_SERVICE).asInstanceOf[InputMethodManager]
        imm.hideSoftInputFromInputMethod(pinTextView.getWindowToken, 0)
        startNextActivity(classOf[UnpluggedBip39MnemonicPhraseActivity])
      }
    } else {
      pin = pinTextView.getEditableText.toString
      startNextActivity(classOf[UnpluggedPINChoiceActivity])
    }
  }

}

