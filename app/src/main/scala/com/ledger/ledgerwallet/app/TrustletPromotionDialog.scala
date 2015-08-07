/**
 *
 * TrustletPromotionDialog
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 31/03/15.
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
package com.ledger.ledgerwallet.app

import android.content.{Intent, Context}
import android.os.Bundle
import android.support.v4.app.FragmentManager
import android.view.{View, ViewGroup, LayoutInflater}
import com.ledger.ledgerwallet.v2.R
import com.ledger.ledgerwallet.base.BaseDialogFragment
import com.ledger.ledgerwallet.remote.HttpClient
import com.ledger.ledgerwallet.utils.Preferenceable
import com.ledger.ledgerwallet.view.DialogActionBarController

import scala.concurrent.{Promise, Future}

class TrustletPromotionDialog extends BaseDialogFragment {

  lazy val actions = DialogActionBarController(R.id.dialog_action_bar).noNeutralButton

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = inflater.inflate(R.layout.trustlet_promotion_dialog, container, false)

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    actions.negativeButton.setText(R.string.dialog_action_no_thanks)
    actions.positiveButton.setText(R.string.dialog_action_see_more)
    actions.onPositiveClick {
      getActivity.startActivity(new Intent(Intent.ACTION_VIEW, Config.TrustletWebPage))
      dismiss()
    }
    actions.onNegativeClick { dismiss() }
  }

}

object TrustletPromotionDialog extends Preferenceable {
  val DefaultTag = "TrustletPromotionDialog"

  override def PreferencesName: String = "TrustletPromotionDialog"

  def isShowable(implicit context: Context): Future[Boolean] = {
    if (preferences.getInt("show_count", 0) > 0)
      Promise().success(false).future
    else
      HttpClient.defaultInstance.testPageExistence(Config.TrustletWebPage.toString)
  }

  def show(fragmentManager: FragmentManager)(implicit context: Context): TrustletPromotionDialog = {
    edit().putInt("show_count", preferences.getInt("show_count", 0) + 1).commit()
    val fragment = new TrustletPromotionDialog()
    fragment.show(fragmentManager, DefaultTag)
    fragment
  }

}