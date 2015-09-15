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

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.view._
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.{BaseActivity, BaseFragment}
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.widget.{TextView, PinTextView}
import android.view.inputmethod.EditorInfo
import android.util.Log
import nordpol.android.{TagDispatcher, OnDiscoveredTagListener}

class UnpluggedPINChoiceActivity extends UnpluggedSetupActivity {

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    val intent = getIntent()

    val bundle = new Bundle()

    intent.hasExtra("pinCode") match {
      case true =>
        val pinCode = intent.getStringExtra("pinCode")
        Log.v("PIN Code", pinCode)
        bundle.putString("pinCode", pinCode)
        bundle.putString("pinChoiceStep", "confirmPIN")
      case _ =>
        bundle.putString("pinChoiceStep", "enterPIN")
    }

    setContentView(R.layout.single_fragment_holder_activity)

    val fragment = new UnpluggedPINChoiceActivityContentFragment()
    fragment.setArguments(bundle)

    getSupportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .commitAllowingStateLoss()
  }

}



class UnpluggedPINChoiceActivityContentFragment extends BaseFragment {

  lazy val pinTextView = TR(R.id.pin_view).as[PinTextView]
  lazy val button = TR(R.id.button).as[TextView]
  lazy val alert = TR(R.id.alert).as[TextView]

  lazy val pinChoiceStep = getArguments().getString("pinChoiceStep");

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.unplugged_pin_choice_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    pinTextView.requestFocus()
    pinTextView.setFocusableInTouchMode(true)
    pinTextView.setOnEditorActionListener((actionId: Int, event: KeyEvent) => {
      actionId match {
        case EditorInfo.IME_ACTION_NEXT =>
          if (pinTextView.getText().length() == 4) {
            pinChoiceStep match {
              case "enterPIN" =>
                button onClick {
                  val intent = new Intent(getActivity, classOf[UnpluggedPINChoiceActivity])
                  intent.putExtra("pinCode", pinTextView.getText().toString)
                  startActivity(intent)
                }
                button.setVisibility(View.VISIBLE)

              case _ =>
                if (pinTextView.getText().toString == getArguments().getString("pinCode")) {
                  alert.setVisibility(View.INVISIBLE)

                  button onClick {
                    val intent = new Intent(getActivity, classOf[UnpluggedRecoveryActivity])
                    intent.putExtra("pinCode", pinTextView.getText().toString)
                    startActivity(intent)
                  }
                  button.setVisibility(View.VISIBLE)
                } else {
                  alert.setVisibility(View.VISIBLE)
                }
            }
          }else{
            button.setVisibility(0)
          }
          true
        case _ => false
      }
    })

  }

}