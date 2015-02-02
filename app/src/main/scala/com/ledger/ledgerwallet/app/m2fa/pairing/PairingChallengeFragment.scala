/**
 *
 * PairingChallengeFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 20/01/15.
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
package com.ledger.ledgerwallet.app.m2fa.pairing

import android.content.Context
import android.os.Bundle
import android.text.{Editable, TextWatcher}
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view.inputmethod.{EditorInfo, InputMethodManager}
import android.view._
import android.widget.RelativeLayout
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.{BaseFragment, ContractFragment}
import com.ledger.ledgerwallet.utils.{Convert, TR}
import com.ledger.ledgerwallet.widget.traits.FontView
import com.ledger.ledgerwallet.widget.{PinTextView, TextView}
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._

class PairingChallengeFragment extends BaseFragment with ContractFragment[CreateDonglePairingActivity.CreateDonglePairingProccessContract] {

  val ExtraChallenge = "ExtraChallenge"

  lazy val PreviousCharacterColor = TR(R.color.dark_grey).asColor
  lazy val CurrentCharacterColor = TR(R.color.invalid_red).asColor
  lazy val NextCharacterColor = TR(R.color.soft_grey).asColor
  lazy val DefaultCharacterFontStyle = FontView.Font.Style.Light
  lazy val CurrentCharacterFontStyle = FontView.Font.Style.SemiBold

  lazy val pinTextView = TR(R.id.pin_view).as[PinTextView]
  lazy val frame = TR(R.id.frame).as[RelativeLayout]
  lazy val bottomText = TR(R.id.bottom_text).as[TextView]
  lazy val letters = {
    val challengeBox = TR(R.id.challenge_box).as[ViewGroup]
    var views = Array[TextView]()
    for (index <- 0 until challengeBox.getChildCount) {
      views = challengeBox.getChildAt(index) match {
        case textView: TextView => views :+ textView
        case _ => views
      }
    }
    views
  }
  private lazy val _challenge = getArguments.getString(ExtraChallenge)

  def this(challenge: String) = {
    this()
    val args = new Bundle()
    args.putString(ExtraChallenge, challenge)
    setArguments(args)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    super.onCreateView(inflater, container, savedInstanceState)
    inflater.inflate(R.layout.pairing_challenge_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    pinTextView.requestFocus()
    pinTextView.setFocusableInTouchMode(true)
    pinTextView.setOnEditorActionListener((actionId: Int, event: KeyEvent) => {
      actionId match {
        case EditorInfo.IME_ACTION_NEXT =>
          nextStep()
          true
        case _ => false
      }
    })
  }

  override def onResume(): Unit = {
    super.onResume()
    updateUI()
    pinTextView.requestFocus()
    pinTextView.postDelayed(pinTextView.requestFocus(), 1000)
    frame.getViewTreeObserver.addOnGlobalLayoutListener(layoutObserver)
    pinTextView.addTextChangedListener(pinTextWatcher)
    activity.map(_.getSystemService(Context.INPUT_METHOD_SERVICE)).foreach(_.asInstanceOf[InputMethodManager].showSoftInput(pinTextView, InputMethodManager.SHOW_IMPLICIT))
  }

  override def onPause(): Unit = {
    super.onPause()
    frame.getViewTreeObserver.removeOnGlobalLayoutListener(layoutObserver)
    pinTextView.removeTextChangedListener(pinTextWatcher)
  }

  private def updateUI(): Unit = {
    val text = pinTextView.getText
    val isSmallScreen = if (frame.getTag != null && frame.getTag.equals("normal")) true else false
    for (i <- 0 until _challenge.length) {
      letters(i).setText(_challenge.charAt(i).toString)
      if (i < text.length()) {
        letters(i).setTextColor(PreviousCharacterColor)
        letters(i).fontStyle = DefaultCharacterFontStyle
        if (isSmallScreen)
          letters(i).setText("")
      } else if (i == text.length()) {
        letters(i).setTextColor(CurrentCharacterColor)
        letters(i).fontStyle = CurrentCharacterFontStyle
      } else {
        letters(i).setTextColor(NextCharacterColor)
        letters(i).fontStyle = DefaultCharacterFontStyle
      }
    }
    if (text.length() < _challenge.length)
      bottomText.setText(
        getResources.getString(R.string.create_dongle_instruction_step_3_bottom,
          _challenge.charAt(text.length).toString)
      )
  }

  val layoutObserver = new OnGlobalLayoutListener {
    override def onGlobalLayout(): Unit = {
        if (frame.getHeight < Convert.dpToPx(200)) {
          bottomText.setVisibility(View.GONE)
        }
        else {
          bottomText.setVisibility(View.VISIBLE)
        }
    }
  }

  val pinTextWatcher = new TextWatcher {
    override def beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int): Unit = {}
    override def onTextChanged(s: CharSequence, start: Int, before: Int, count: Int): Unit = {}
    override def afterTextChanged(s: Editable): Unit = {
      updateUI()
      if (s.length() == _challenge.length)
        nextStep()
    }
  }

  def nextStep(): Unit = {
    if (pinTextView.getText().length() == _challenge.length)
      contract.setChallengeAnswer(pinTextView.getText().toString)
  }

  override def tag: String = "PairingChallengeFragment"
}