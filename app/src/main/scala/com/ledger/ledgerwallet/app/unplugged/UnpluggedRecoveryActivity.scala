/**
 *
 * HomeActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 10/02/15.
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
package com.ledger.ledgerwallet.app.unplugged

import android.content.Intent
import android.os.Bundle
import android.view._
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.{BaseActivity, BaseFragment}
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.widget.{EditText, TextView}
import android.view.inputmethod.EditorInfo
import android.util.Log


class UnpluggedRecoveryActivity extends BaseActivity {
  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)

    val intent = getIntent()
    val bundle = new Bundle()
    intent.hasExtra("pinCode") match {
      case true =>
        bundle.putString("pinCode", intent.getStringExtra("pinCode"))
      case _ => /* Error */
    }

    setContentView(R.layout.single_fragment_holder_activity)

    val fragment = new UnpluggedRecoveryActivityContentFragment()
    fragment.setArguments(bundle)

    getSupportFragmentManager
      .beginTransaction()
      .replace(R.id.fragment_container, fragment)
      .commitAllowingStateLoss()
  }
}



class UnpluggedRecoveryActivityContentFragment extends BaseFragment {

  lazy val button = TR(R.id.button).as[TextView]
  lazy val recovery = TR(R.id.recovery).as[EditText]

  lazy val pinCode = getArguments().getString("pinCode")

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.unplugged_recovery_fragment, container, false)
  }

  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)

    button onClick {
      val intent = new Intent(getActivity, classOf[UnpluggedReadyToInstallActivity])
      intent.putExtra("pinCode", pinCode)
      Log.v("recovery", recovery.getText().toString())
      intent.putExtra("recovery", recovery.getText().toString)
      startActivity(intent)
    }
  }

}