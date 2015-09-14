/**
 *
 * RecoveryTextView
 * Ledger wallet
 *
 * Created by Julien BENOIT on 14/09/15.
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
import android.graphics.{RectF, Paint, Color, Canvas}
import android.os.Build
import android.text.InputFilter
import android.util.AttributeSet
import android.view.View.MeasureSpec
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.utils.Convert

class RecoveryTextView(context: Context, attrs: AttributeSet) extends EditText(context, attrs) {
  lazy val DefaultBoxHeight = Convert.dpToPx(40)
  lazy val DefaultBoxWidth = Convert.dpToPx(150)
  lazy val DefaultBoxMargin = Convert.dpToPx(10)
  lazy val DefaultBoxRadius = Convert.dpToPx(10)
  lazy val DefaultBoxBorderThickness = Convert.dpToPx(1)
  lazy val DefaultPlaceHolderSize = Convert.dpToPx(15)

  val a = context.obtainStyledAttributes(attrs, R.styleable.RecoveryTextView)
  val numberOfDigits = a.getInt(R.styleable.RecoveryTextView_numberOfDigits2, 1)

  private var _boxWidth = DefaultBoxWidth
  private var _boxHeight = DefaultBoxHeight
  private val _boxMargin = DefaultBoxMargin
  private val _borderThickness = DefaultBoxBorderThickness
  private val _placeHolderSize = DefaultPlaceHolderSize

  private val _boxBackgroundPaint = {
    val p = new Paint()
    p.setColor(Color.WHITE)
    p.setAntiAlias(true)
    p
  }
  private val _boxInactiveBorderPaint = {
    val p = new Paint()
    p.setColor(Color.rgb(0xC9, 0xC9, 0xC9))
    p.setAntiAlias(true)
    p
  }
  private val _boxActiveBorderPaint = {
    val p = new Paint()
    p.setColor(Color.rgb(0xEA, 0x2E, 0x49))
    p.setAntiAlias(true)
    p
  }
  private val _boxFilledBorderPaint = {
    val p = new Paint()
    p.setColor(Color.rgb(0x66, 0x66, 0x66))
    p.setAntiAlias(true)
    p
  }
  private val _boxRect = new RectF()


  override def onSelectionChanged(selStart: Int, selEnd: Int): Unit = {
    if (getText != null && (selStart != getText.length() || selEnd != getText.length()))
      setSelection(getText.length())
    super.onSelectionChanged(selStart, selEnd)
  }

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
    var i = 0
    while (i < numberOfDigits) {
      drawLetterBox(canvas, i)
      i += 1
    }
  }

  private def drawLetterBox(canvas: Canvas, index: Int): Unit = {
    val left = index * _boxWidth + index * _boxMargin
    var borderPaint: Paint = null

    if (getSelectionStart == index)
      borderPaint = _boxActiveBorderPaint
    else if (index < getSelectionStart)
      borderPaint = _boxFilledBorderPaint
    else
      borderPaint = _boxInactiveBorderPaint

    _boxRect.set(
      left,
      0,
      left + _boxWidth,
      _boxHeight
    )
    canvas.drawRoundRect(_boxRect, DefaultBoxRadius, DefaultBoxRadius, borderPaint)

    _boxRect.set(
      left + _borderThickness,
      _borderThickness,
      left + _boxWidth - _borderThickness,
      _boxHeight - _borderThickness
    )
    canvas.drawRoundRect(_boxRect, DefaultBoxRadius, DefaultBoxRadius, _boxBackgroundPaint)
    if (index < getSelectionStart)
      canvas.drawCircle(left + _boxWidth / 2f, _boxHeight / 2, _placeHolderSize / 2f, borderPaint)
  }

  private def initialize(): Unit = {
    setCursorVisible(false)
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN)
      setBackgroundDrawable(null)
    else
      setBackground(null)
    setFilters(getFilters :+ new InputFilter.LengthFilter(numberOfDigits))
  }


  initialize()
}