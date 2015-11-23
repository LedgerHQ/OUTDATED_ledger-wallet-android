/**
 *
 * DemoActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/11/15.
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
package co.ledger.wallet.app

import java.util.concurrent.Executor

import android.os.{Looper, Handler, AsyncTask, Bundle}
import co.ledger.wallet.R
import co.ledger.wallet.core.base.BaseActivity
import co.ledger.wallet.core.utils.TR
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.common._

class DemoActivity extends BaseActivity {

  lazy val text = TR(R.id.text).as[TextView]

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    Logger.d("UI initialized")
    setContentView(R.layout.unplugged_welcome_activity)
    text.setText("onCreate")
    new Thread() {
      override def run(): Unit = {
        super.run()
        ec.execute(new Runnable {
          override def run(): Unit = append("Running from thread")
        })
      }
    } start()
  }

  def append(text: String): Unit = {
    this.text.setText(this.text.getText.toString + "\n" + text)
  }

}
