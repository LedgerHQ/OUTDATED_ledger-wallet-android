/**
 *
 * UnpluggedSetupKeyCardActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 16/09/15.
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

import java.util

import android.app.{Dialog, DialogFragment}
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.net.Uri
import android.os.Bundle
import android.support.v7.app.AlertDialog
import android.support.v7.app.AlertDialog.Builder
import android.text.InputFilter.LengthFilter
import android.text.InputType
import android.view.View
import android.view.ViewGroup.LayoutParams
import android.widget.{Toast, LinearLayout, RelativeLayout}
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.nfc.Utils
import com.ledger.ledgerwallet.utils.{AndroidUtils, TR}
import com.ledger.ledgerwallet.widget.{EditText, TextView}
import me.dm7.barcodescanner.zbar.ZBarScannerView.ResultHandler
import me.dm7.barcodescanner.zbar.{BarcodeFormat, Result, ZBarScannerView}

import scala.util.Try

class UnpluggedSetupKeyCardActivity extends UnpluggedSetupActivity with ResultHandler {

  val KeycardGeneratorUri = Uri.parse("https://www.ledgerwallet.com/wallet/keycard")

  private lazy val scannerView = new ZBarScannerView(this)
  private lazy val scannerLayout = TR(R.id.scanner_layout).as[RelativeLayout]
  private lazy val manualSeedButton = TR(R.id.button).as[TextView]
  private lazy val createKeycardLink = TR(R.id.create_keycard).as[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.unplugged_setup_keycard_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)

    scannerLayout.addView(scannerView, 0)

    stepNumberTextView.setText(R.string.unplugged_scan_step_number)
    stepInstructionTextView.setText(R.string.unplugged_scan_step_instruction)

    scannerView.removeViewAt(1)

    manualSeedButton onClick {
      new SeedPromptAlertDialogFragment().show(getFragmentManager, "KeycardPrompt")
    }

    createKeycardLink onClick AndroidUtils.startBrowser(KeycardGeneratorUri)

  }

  override def onResume(): Unit = {
    super.onResume()
    val formats = new util.ArrayList[BarcodeFormat](1)
    formats.add(BarcodeFormat.QRCODE)
    scannerView.setResultHandler(this)
    scannerView.setAutoFocus(true)
    scannerView.setFormats(formats)
    scannerView.startCamera()
  }


  override def onPause(): Unit = {
    super.onPause()
    scannerView.stopCamera()
  }

  override def handleResult(result: Result): Unit = {
    if (!onSeedIsProvided(result.getContents)) {
      scannerView.startCamera()
    }
  }

  private[this] def onSeedIsProvided(seed: String): Boolean = {
    if (seed.length == 32 && Try(Utils.decodeHex(seed)).isSuccess) {
      keycardSeed = seed
      startNextActivity(classOf[UnpluggedFinalizeSetupActivity])
      true
    } else {
      false
    }
  }

  class SeedPromptAlertDialogFragment extends DialogFragment {

    private lazy val inputText = getDialog.findViewById(R.id.edit_text).asInstanceOf[EditText]

    override def onCreateDialog(savedInstanceState: Bundle): Dialog = {
      new Builder(getActivity)
        .setTitle(R.string.unplugged_scan_dialog_title)
        .setMessage(R.string.unplugged_scan_dialog_message)
        .setPositiveButton(android.R.string.ok, new OnClickListener {
        override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {}
      }).setNegativeButton(android.R.string.cancel, new OnClickListener {
        override def onClick(dialogInterface: DialogInterface, i: Int): Unit = {}
      }).setView(R.layout.dialog_edittext)
      .create()
    }

    override def onResume(): Unit = {
      super.onResume()
      val dialog = getDialog.asInstanceOf[AlertDialog]
      dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener {
        override def onClick(view: View): Unit = {
          if (onSeedIsProvided(inputText.getText.toString)) {
            dismiss()
          } else {
            Toast.makeText(getActivity, R.string.unplugged_scan_dialog_error, Toast.LENGTH_LONG).show()
          }
        }
      })
    }

  }

}
