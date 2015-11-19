/**
 *
 * LetterSpacingSpan.scala
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 13/01/15.
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

package co.ledger.wallet.core.style

import android.graphics.Paint.FontMetricsInt
import android.graphics.{Typeface, Canvas, Paint}
import android.text.style.ReplacementSpan

class LetterSpacingSpan(typeface: Typeface, val letterSpacing: Float) extends TypefaceSpan(typeface) {

  override def getSize(paint: Paint, text: CharSequence, start: Int,
                       end: Int, fm: FontMetricsInt)
  : Int = {
    (paint.measureText(text, start, end) + letterSpacing * (end - start - 1)).asInstanceOf[Int]
  }

  override def draw(canvas: Canvas, text: CharSequence, start: Int,
                    end: Int, x: Float, top: Int, y: Int, bottom: Int,
                    paint: Paint)
  : Unit = {
    var dx = x
    for (i <- start until end) {
      canvas.drawText(text, i, i + 1, dx, y, paint)
      dx += paint.measureText(text, i, i + 1) + letterSpacing
    }
  }
}