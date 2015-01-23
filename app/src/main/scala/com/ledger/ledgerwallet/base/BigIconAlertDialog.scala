/**
 *
 * BigIconAlertDialog
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 23/01/15.
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
package com.ledger.ledgerwallet.base

import android.content.Context
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.{View, ViewGroup, LayoutInflater}
import android.widget.ImageView
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.view.DialogActionBarController
import com.ledger.ledgerwallet.widget.TextView

class BigIconAlertDialog extends BaseDialogFragment {

  private var _title: CharSequence = _
  private var _content: CharSequence = _
  private var _icon: Drawable = _

  private lazy val titleView = TR(R.id.title).as[TextView]
  private lazy val contentView = TR(R.id.content).as[TextView]
  private lazy val iconView = TR(R.id.icon).as[ImageView]
  private lazy val actionBar = DialogActionBarController(R.id.dialog_action_bar).noPositiveButton.noNeutralButton

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.bigicon_dialog, container, false)
  }


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    actionBar onNegativeClick dismiss
    actionBar.negativeButton.setText(R.string.dialog_action_close)
    updateUI()
  }

  def title_=(title: CharSequence): Unit = {
    _title = title
    updateUI()
  }

  def content_=(content: CharSequence): Unit = {
    _content = content
    updateUI()
  }

  def icon_=(icon: Drawable): Unit = {
    _icon = icon
    updateUI()
  }

  protected def updateUI(): Unit = {
     if (getView == null)
       return
    titleView.setText(_title)
    contentView.setText(_content)
    iconView.setImageDrawable(_icon)
  }

}

object BigIconAlertDialog {

  class Builder(c: Context) {

    private implicit val context = c
    private var _title: CharSequence = _
    private var _content: CharSequence = _
    private var _icon: Drawable = _

    def setTitle(stringId: Int): Builder = setTitle(TR(stringId).as[String])
    def setTitle(title: CharSequence): Builder = {
      _title = title
      this
    }

    def setContentText(stringId: Int): Builder = setContentText(TR(stringId).as[String])
    def setContentText(content: CharSequence): Builder = {
      _content = content
      this
    }

    def setIcon(resId: Int): Builder = setIcon(TR(resId).as[Drawable])
    def setIcon(icon: Drawable): Builder = {
      _icon = icon
      this
    }

    def create(): BigIconAlertDialog = {
      val dialog = new BigIconAlertDialog
      dialog._title = _title
      dialog._content = _content
      dialog._icon = _icon
      dialog
    }

  }

}