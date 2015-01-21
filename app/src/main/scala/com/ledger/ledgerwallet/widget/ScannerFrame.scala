/**
 *
 * ScannerFrame
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
import android.graphics._
import android.view.View
import com.ledger.ledgerwallet.utils.Convert

class ScannerFrame(context: Context) extends View(context) {

  private val _maskPaint = {
    val p = new Paint()
    p.setColor(Color.argb(76, 0, 0, 0))
    p
  }

  private val _aimingWhiteFrame = {
    val p = new Paint()
    p.setAntiAlias(true)
    p.setColor(Color.argb(191, 255, 255, 255))
    p
  }

  private val _aimingFrameRadius = Convert.dpToPx(5)
  private val _aimingFrameBorderSize = Convert.dpToPx(40)
  private val _aimingFrameBorderThickness = Convert.dpToPx(6)

  private val _cutPaint = {
    val p = new Paint()
    p.setColor(Color.TRANSPARENT)
    p.setAntiAlias(true)
    p.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.CLEAR))
    p
  }

  private val _roundedWhiteRect = new RectF()
  private val _roundedRectCut = new RectF()

  override def onDraw(canvas: Canvas): Unit = {
    super.onDraw(canvas)
    drawMask(canvas)
    drawAimingFrame(canvas)
  }

  private def drawMask(canvas: Canvas): Unit = {
    canvas.drawRect(0f, 0f, canvas.getWidth, canvas.getHeight, _maskPaint)
  }

  private def drawAimingFrame(canvas: Canvas): Unit = {
    val width = canvas.getWidth
    val height = canvas.getHeight

    val frameSize = width * 0.60f
    val left = (width - frameSize) / 2
    val top = (height - frameSize) / 2

    _roundedWhiteRect.set(
      left,
      top,
      left + frameSize,
      top + frameSize
    )

    canvas.drawRoundRect(
      _roundedWhiteRect,
      _aimingFrameRadius,
      _aimingFrameRadius,
      _aimingWhiteFrame
    )

    _roundedRectCut.set(
      left + _aimingFrameBorderThickness,
      top + _aimingFrameBorderThickness,
      left + frameSize - _aimingFrameBorderThickness,
      top + frameSize - _aimingFrameBorderThickness
    )

    canvas.drawRoundRect(
      _roundedRectCut,
      _aimingFrameRadius,
      _aimingFrameRadius,
      _cutPaint
    )

    canvas.drawRect(
      left,
      top + _aimingFrameBorderSize,
      left + frameSize,
      top + frameSize - _aimingFrameBorderSize,
      _cutPaint
    )

    canvas.drawRect(
      left + _aimingFrameBorderSize,
      top - 1,
      left + frameSize - _aimingFrameBorderSize,
      top + frameSize + 1,
      _cutPaint
    )

  }

}
