/**
 *
 * BaseActivity.scala
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 09/01/15.
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

package co.ledger.wallet.core.base

import android.app.NotificationManager
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.{AppCompatActivity, ActionBar, ActionBarActivity}
import co.ledger.wallet.core.concurrent.ExecutionContext
import co.ledger.wallet.core.concurrent.ExecutionContext.UiContextExecutionContext
import co.ledger.wallet.core.widget.Toolbar
import android.view.ViewGroup.LayoutParams
import android.view.{MenuItem, LayoutInflater, View}
import android.widget.FrameLayout
import co.ledger.wallet.R
import co.ledger.wallet.core.utils.{AndroidUtils, TR}
import co.ledger.wallet.core.utils.logs.Loggable

import android.content.Context
import co.ledger.wallet.service.gcm.GcmIntentService

abstract class BaseActivity extends AppCompatActivity with Loggable with UiContext {
  implicit val context = this
  implicit val ec = scala.concurrent.ExecutionContext.fromExecutor(new UiContextExecutionContext
  (this))

  lazy val toolbar = TR(R.id.toolbar).as[Toolbar]
  lazy val content = TR(R.id.content_view).as[FrameLayout]

  private[this] var _visible = false

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    super.setContentView(R.layout.base_activity)
    setSupportActionBar(toolbar)
    toolbar.style = actionBarStyle
    if (actionBarStyle == Toolbar.Style.Normal) {
      getSupportActionBar.setDisplayShowTitleEnabled(false)
      getSupportActionBar.setDisplayShowCustomEnabled(true)
      getSupportActionBar.setCustomView(toolbar.titleView)
    }
    toolbar.setTitle(getTitle)
  }

  override def setTitle(titleId: Int): Unit = {
    super.setTitle(titleId)
    toolbar.setTitle(getTitle)
  }

  override def onResume(): Unit = {
    super.onResume()
    _visible = true
    AndroidUtils.notifyActivityOnResume()
    getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
      .cancel(GcmIntentService.IncomingTransactionNotificationId)
  }

  override def onPause(): Unit = {
    super.onPause()
    _visible = false
    AndroidUtils.notifyActivityOnPause()
  }

  override def setContentView(layoutResID: Int): Unit = {
    val inflater = LayoutInflater.from(this)
    val view = inflater.inflate(layoutResID, content, false)
    setContentView(view, view.getLayoutParams())
  }

  override def setContentView(view: View): Unit = {
    setContentView(view, new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
  }

  def setContentFragment(baseFragment: Fragment): Unit = {
    val ft = getSupportFragmentManager.beginTransaction()
    ft.replace(R.id.content_view, baseFragment)
    ft.commit()
  }

  override def setContentView(view: View, params: LayoutParams): Unit = {
    content.removeAllViews()
    content.addView(view, params)
  }


  def runOnUiThread(f: => Unit): Unit = {
    runOnUiThread(new Runnable {
      override def run(): Unit = f
    })
  }

  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    if ((item.getItemId == android.R.id.home && !onClickHome()) || item.getItemId != android.R.id.home)
      super.onOptionsItemSelected(item)
    else
      true
  }

  def onClickHome(): Boolean = {
    finish()
    true
  }

  def actionBarStyle: Toolbar.Style = Toolbar.Style.Normal

  override def isVisible: Boolean = _visible

  override def isHidden: Boolean = !isVisible
}