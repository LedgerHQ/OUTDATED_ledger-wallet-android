/**
 *
 * Toolbar
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 14/01/15.
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
import android.support.v7.app.ActionBar

import android.util.AttributeSet
import android.view.ViewGroup.LayoutParams
import android.view.{ViewGroup, View, LayoutInflater}
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.BaseActivity
import com.ledger.ledgerwallet.utils.TR

class Toolbar(context: Context, attrs: AttributeSet) extends android.support.v7.widget.Toolbar(context, attrs) {

  private lazy val _titleTextView = TR(titleView, R.id.action_bar_title).as[TextView]
  private lazy val _subtitleTextView = TR(titleView, R.id.action_bar_subtitle).as[TextView]
  private var _titleView: View = _
  private var _title: Option[CharSequence] = None
  private var _subtitle: Option[CharSequence] = None
  def titleView = _titleView

  override def setTitle(title: CharSequence): Unit = {
    _title = Option(title)
    if (_titleView != null) {
      _titleTextView.setText(title)
    }
    updateUI()
  }

  override def setSubtitle(subtitle: CharSequence): Unit = {
    _subtitle = Option(subtitle)
    if (_titleView != null) {
      _subtitleTextView.setText(subtitle)
    }
    updateUI()
  }

  private def updateUI(): Unit = {
    if (_titleView == null)
      return
    _subtitleTextView.setVisibility(if (_subtitle.isEmpty) View.GONE else View.VISIBLE)
  }

  private var _style = Toolbar.Style.Normal
  def style = _style
  def style_=(newStyle: Toolbar.Style) = {
    _style = newStyle
    val inflater = LayoutInflater.from(getContext)
    _titleView = _style match {
      case Toolbar.Style.Normal => inflater.inflate(R.layout.action_bar_title, this, false)
      case Toolbar.Style.Expanded => inflater.inflate(R.layout.expanded_action_bar_title, this, false)
    }
    if (_style == Toolbar.Style.Expanded) {
      addView(titleView)
    }
    setTitle(_title.orNull)
    setSubtitle(_subtitle.orNull)
  }

}

object Toolbar {

  type Style = Int

  object Style {
    val Normal = 0
    val Expanded = 1
  }

}