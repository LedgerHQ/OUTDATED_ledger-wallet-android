/**
 *
 * FontView.scala
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

package com.ledger.ledgerwallet.widget.traits

import android.content.Context
import android.graphics.Typeface
import android.os.Looper
import android.text.style.ScaleXSpan
import android.text.{SpannableString, Spannable, SpannableStringBuilder, Spanned}
import android.util.AttributeSet
import android.view.View
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.utils.logs.Logger

import scala.collection.mutable

trait FontView {

  private var _typeface: Option[Typeface] = None
  def typeface = _typeface
  private var _fontFamily =  FontView.Font.Family.OpenSans
  private var _fontStyle = FontView.Font.Style.Regular
  def fontFamily = _fontFamily
  def fontStyle = _fontStyle

  private var _kerning: Float = 0
  private var _originalCharSequence: Option[CharSequence] = None
  def kerning = _kerning
  private var _initialized = false

  protected def initializeFontView(context: Context, attrs: AttributeSet): Unit = {
    _initialized = true
    val a = context.obtainStyledAttributes(attrs, Array(R.attr.fontFamily, R.attr.fontStyle, R.attr.kerning))
    _fontFamily = FontView.Font.Family(a.getInt(0, _fontFamily.id))
    _fontStyle = FontView.Font.Style(a.getInt(1, _fontStyle.id))
    _originalCharSequence = Option(getText())
    kerning = a.getDimension(2, 0)
    typeface = FontView.loadTypefaceFromAssets(this.asInstanceOf[View], _fontFamily, _fontStyle)
  }

  def invalidate(): Unit
  def postInvalidate(): Unit
  def getContext(): Context
  def setTypeface(typeface: Typeface): Unit
  def getText():CharSequence
  def isInEditMode():Boolean

  protected def requestInvalidate(): Unit = {
    if (Looper.getMainLooper() == Looper.myLooper()) {
      invalidate()
    } else {
      postInvalidate()
    }
  }

  def fontFamily_=(fontFamily: FontView.Font.Family.Family): Unit = {
   if (fontFamily == _fontFamily)
     return
    _fontFamily = fontFamily
    typeface = FontView.loadTypefaceFromAssets(this.asInstanceOf[View], _fontFamily, _fontStyle)
  }

  def fontStyle_=(fontStyle: FontView.Font.Style.Style):Unit = {
    if (fontStyle != _fontStyle)
      return
    _fontStyle = fontStyle
    typeface = FontView.loadTypefaceFromAssets(this.asInstanceOf[View], _fontFamily, _fontStyle)
  }

  def kerning_=(newKerning: Float):Unit = {
    _kerning = newKerning
    _originalCharSequence.foreach { requestCharacterStyleComputation }
  }

  def typeface_=(typeface: Option[Typeface]):Unit = {
    _typeface = typeface
    _typeface.foreach { setTypeface }
    requestInvalidate()
  }

  protected def requestCharacterStyleComputation(charSequence: CharSequence): Unit = {
    if (!_initialized) {
      onCharacterStyleChanged(new SpannableStringBuilder(charSequence))
      return
    }
    _originalCharSequence = Option(charSequence)
    if (_originalCharSequence forall { _.length() == 0 }) {
      onCharacterStyleChanged(null)
      return
    }
    val builder = new SpannableStringBuilder()
    var length = _originalCharSequence.get.length()
    for (i <- 0 until length) {
      builder.append(_originalCharSequence.get.charAt(i))
      if (i + 1 < length)
        builder.append("\u00A0")
    }

    val finalSequence = new SpannableString(builder)
    length = builder.toString.length
    for (i <- 1 until length by 2) {
      val span = new ScaleXSpan((_kerning + 1) / 10)
      finalSequence.setSpan(span, i, i + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }

    onCharacterStyleChanged(finalSequence)
    requestInvalidate()
  }

  protected def onCharacterStyleChanged(span: Spannable):Unit


}

object FontView {

  object Font {

    object Family extends Enumeration {
      type Family = Value
      val OpenSans = Value("opensans")
    }

    object Style extends Enumeration {
      type Style = Value
      val Light = Value("light")
      val Regular = Value("regular")
      val SemiBold = Value("semibold")
      val Bold = Value("bold")
      val ExtraBold = Value("extrabold")
      val Italic = Value("italic")
    }

    val DefaultFamily = Family.OpenSans
    val DefaultStyle = Style.Regular

  }

  private val TypeFaces = new mutable.HashMap[String, Typeface]()

  def loadTypefaceFromAssets(
                              view: View,
                              fontFamily: Font.Family.Family,
                              fontStyle: Font.Style.Style)
  :Option[Typeface] = {
    if (view.isInEditMode)
      return Option(Typeface.DEFAULT)
    val path = "fonts/" + fontFamily.toString + "-" + fontStyle.toString + ".ttf"
    if (TypeFaces.contains(path)) {
      Some(TypeFaces(path))
    } else {
      try {
        val typeface = Typeface.createFromAsset(view.getContext.getAssets, path)
        TypeFaces(path) = typeface
        Option(typeface)
      } catch {
        case e: Exception => {
          Logger.e("Unable to create font from \"" + path + "\"")
          None
        }
      }
    }
  }

}