/**
 *
 * PinTextView
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
package com.ledger.ledgerwallet.widget

import android.content.Context
import android.graphics.{Color, Canvas}
import android.os.Build
import android.util.AttributeSet
import android.view.View.MeasureSpec
import android.widget.EditText
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.utils.Convert

class PinTextView(context: Context, attrs: AttributeSet) extends EditText(context, attrs) {
  lazy val DefaultBoxHeight = Convert.dpToPx(75)
  lazy val DefaultBoxWidth = Convert.dpToPx(55)
  lazy val DefaultBoxMargin = Convert.dpToPx(10)

  setCursorVisible(false)
  if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
    setBackgroundDrawable(null)
  else
    setBackground(null)

  val a = context.obtainStyledAttributes(attrs, R.styleable.PinTextView)

  val numberOfDigits = a.getInt(R.styleable.PinTextView_numberOfDigits, 4)

  private var _boxWidth = DefaultBoxWidth
  private var _boxHeight = DefaultBoxHeight

  override def onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int): Unit = {
    val widthMode = MeasureSpec.getMode(widthMeasureSpec)
    val heightMode = MeasureSpec.getMode(heightMeasureSpec)
    var width = MeasureSpec.getSize(widthMeasureSpec)
    var height = MeasureSpec.getSize(heightMeasureSpec)

    val measureWidth = () => DefaultBoxWidth * numberOfDigits + DefaultBoxMargin * (numberOfDigits - 1)
    val measureHeight = () => DefaultBoxHeight

    width = widthMode match {
      case MeasureSpec.EXACTLY => width
      case MeasureSpec.AT_MOST => Math.min(measureWidth().asInstanceOf[Int], width)
      case _ => measureWidth().asInstanceOf[Int]
    }

    height = heightMode match {
      case MeasureSpec.EXACTLY => height
      case MeasureSpec.AT_MOST => Math.min(measureHeight().asInstanceOf[Int], width)
      case _ => measureHeight().asInstanceOf[Int]
    }

    _boxWidth = (width - DefaultBoxMargin * (numberOfDigits - 1)) / numberOfDigits
    _boxHeight = height

    setMeasuredDimension(width, height)
  }

  override def onDraw(canvas: Canvas): Unit = {
    canvas.drawColor(Color.RED)
  }
}