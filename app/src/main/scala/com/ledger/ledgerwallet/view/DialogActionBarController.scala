/**
 *
 * DialogActionBarController
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 22/01/15.
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
package com.ledger.ledgerwallet.view

import android.support.v4.app.DialogFragment
import android.view.{View, ViewGroup}
import com.ledger.ledgerwallet.v2.R
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.TextView
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._

class DialogActionBarController(actionBarView: ViewGroup) {

  implicit val context = actionBarView.getContext
  implicit val view = actionBarView

  lazy val positiveButton = TR(R.id.positive_button).as[TextView]
  lazy val neutralButton = TR(R.id.neutral_button).as[TextView]
  lazy val negativeButton = TR(R.id.negative_button).as[TextView]

  def noNeutralButton: DialogActionBarController = {
    neutralButton.setVisibility(View.GONE)
    this
  }

  def noNegativeButton: DialogActionBarController = {
    negativeButton.setVisibility(View.GONE)
    this
  }

  def noPositiveButton: DialogActionBarController = {
    positiveButton.setVisibility(View.GONE)
    this
  }

  def onPositiveClick[F](f: => F): DialogActionBarController = {
    positiveButton.setOnClickListener(f)
    this
  }

  def onNeutralClick[F](f: => F): DialogActionBarController = {
    neutralButton.setOnClickListener(f)
    this
  }

  def onNegativeClick[F](f: => F): DialogActionBarController = {
    negativeButton.setOnClickListener(f)
    this
  }

}

object DialogActionBarController {

  def apply(actionBarViewId: Int)(implicit dialog: DialogFragment): DialogActionBarController = {
    new DialogActionBarController(dialog.getView.findViewById(actionBarViewId).asInstanceOf[ViewGroup])
  }

}
