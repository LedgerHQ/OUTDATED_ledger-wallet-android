/**
 *
 * UnpluggedBip39MnemonicPhraseActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 15/09/15.
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
import com.ledger.ledgerwallet.bitcoin.Bip39Helper
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.{Bip39MnemonicPhraseEditText, TextView}
import com.ledger.ledgerwallet.R

class UnpluggedBip39MnemonicPhraseActivity extends UnpluggedSetupActivity {

  lazy val button = TR(R.id.button).as[TextView]
  lazy val seedEditText = TR(R.id.phrase).as[Bip39MnemonicPhraseEditText]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_bip39_phrase_fragment)

    if (isInCreationMode) {
      stepNumberTextView.setText(R.string.unplugged_seed_header_step_number_create)
      stepInstructionTextView.setText(R.string.unplugged_seed_header_step_text_create)
      seedEditText.setText(Bip39Helper.generateMnemonicPhrase())
      seedEditText.setEnabled(false)
    } else {
      stepNumberTextView.setText(R.string.unplugged_seed_header_step_number_restore)
      stepInstructionTextView.setText(R.string.unplugged_seed_header_step_text_restore)
      seedEditText.setText("")
    }

    button onClick {
      mnemonicPhrase = seedEditText.getEditableText.toString
      if (!Bip39Helper.isMnemomicPhraseValid(mnemonicPhrase.get)) {
        Toast.makeText(this, R.string.unplugged_seed_invalid_seed, Toast.LENGTH_LONG).show()
      } else {
        startNextActivity(classOf[UnpluggedFinalizeSetupActivity])
      }
    }

  }

}
