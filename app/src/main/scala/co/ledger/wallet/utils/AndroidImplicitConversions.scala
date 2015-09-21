/**
 *
 * AndroidImplicitConversions
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 19/01/15.
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
package co.ledger.wallet.utils

import android.view.View.OnClickListener
import android.view.{KeyEvent, View}
import android.widget.TextView
import android.widget.TextView.OnEditorActionListener

trait AndroidImplicitConversions {

  implicit def funcToViewOnClickListener[F](f: => F): View.OnClickListener = {
    new View.OnClickListener {
      override def onClick(v: View): Unit = f
    }
  }

  implicit def funcToViewOnClickListener[F](f: (View) => F): View.OnClickListener = {
    new View.OnClickListener {
      override def onClick(v: View): Unit = f(v)
    }
  }

  implicit def funcToRunnable[F](f: => F): Runnable = {
    new Runnable {
      override def run(): Unit = f
    }
  }

  implicit def funcToOnEditorActionListener(f: (Int, KeyEvent) => Boolean): OnEditorActionListener = {
    new OnEditorActionListener {
      override def onEditorAction(v: TextView, actionId: Int, event: KeyEvent): Boolean = f(actionId, event)
    }
  }

  implicit class RichView(v: View) {

    def onClick(c: => Unit): Unit = {
      v.setOnClickListener(new OnClickListener {
        override def onClick(v: View): Unit = c
      })
    }

  }

}

object AndroidImplicitConversions extends AndroidImplicitConversions
