/**
 *
 * TextView.scala
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 12/01/15.
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

package com.ledger.ledgerwallet.widget

import android.content.Context
import android.graphics.Typeface
import android.text.Spannable
import android.util.AttributeSet
import android.widget.TextView.BufferType
import com.ledger.ledgerwallet.widget.traits.FontView

class EditText(context: Context, attrs: AttributeSet, defStyleAttr: Int, defStyleRes: Int)
  extends android.widget.EditText(context, attrs)
  with FontView {
  initializeFontView(context, attrs)

  def this(context: Context, attrs: AttributeSet, defStyleAttr: Int) = this(context, attrs, 0, 0)
  def this(context: Context, attrs: AttributeSet) = this(context, attrs, 0)
  def this(context: Context) = this(context, null)

  override def setTypeface(tf: Typeface): Unit = {
    super.setTypeface(tf)
  }

  override def setText(text: CharSequence, `type`: BufferType): Unit = {
    requestCharacterStyleComputation(text)
  }

  override protected def onCharacterStyleChanged(span: Spannable): Unit = {
    super.setText(span, BufferType.SPANNABLE)
  }
}