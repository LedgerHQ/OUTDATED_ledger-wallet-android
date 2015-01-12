/**
 *
 * ${FILE_NAME}
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

package com.ledger.ledgerwallet.utils

import android.app.Activity
import android.content.Context
import android.support.v4.app.Fragment
import android.view.View

import scala.reflect.ClassTag

class TR(id: Int, context: AnyRef) {

  def as[A](implicit ct: ClassTag[A]): A = {
    context match {
      case v: View => fromView(v, ct)
      case a: Activity => fromActivity(a, ct)
      case f: Fragment => fromFragment(f, ct)
      case c: Context => fromContext(c, ct)
    }
  }

  private def fromView[A](v: View, classTag: ClassTag[A]): A = {
    classTag match {
      case view if classOf[View].isAssignableFrom(classTag.runtimeClass) => v.findViewById(id).asInstanceOf[A]
      case _ => fromContext(v.getContext, classTag)
    }
  }

  private def fromActivity[A](a: Activity, classTag: ClassTag[A]): A = {
    classTag match {
      case view if classOf[View].isAssignableFrom(classTag.runtimeClass) => a.findViewById(id).asInstanceOf[A]
      case _ => fromContext(a, classTag)
    }
  }

  private def fromFragment[A](f: Fragment, classTag: ClassTag[A]): A = {
    classTag match {
      case view if classOf[View].isAssignableFrom(classTag.runtimeClass) => f.getView.findViewById(id).asInstanceOf[A]
      case _ => fromContext(f.getActivity, classTag)
    }
  }

  private def fromContext[A](c: Context, classTag: ClassTag[A]): A = {
    classTag match {
      case string if classOf[String] == classTag.runtimeClass => c.getResources.getString(id).asInstanceOf[A]
      case int if classOf[Int] == classTag.runtimeClass => c.getResources.getDimension(id).asInstanceOf[A]
    }
  }

}

object TR {

  def apply(id: Int)(implicit context: Context, fragment: Fragment = null, view: View = null): TR = new TR(id, getHighestPriorityArgument(context, fragment, view))
  def apply(context: Context, id: Int): TR = new TR(id, context)

  def getHighestPriorityArgument(args: AnyRef*): AnyRef = {
    var out: AnyRef = null
    for (arg <- args) {
      if (arg != null)
        out = arg
    }
    out
  }

}