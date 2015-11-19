/**
 *
 * NameDongleFragment
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 21/01/15.
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
package co.ledger.wallet.app.ui.m2fa.pairing

import android.os.Bundle
import android.view.ViewTreeObserver.OnGlobalLayoutListener
import android.view._
import android.view.inputmethod.EditorInfo
import co.ledger.wallet.R
import co.ledger.wallet.app.base.{ContractFragment, BaseFragment}
import co.ledger.wallet.models.PairedDongle
import co.ledger.wallet.core.utils.{Convert, TR}
import co.ledger.wallet.core.widget.{TextView, EditText}
import co.ledger.wallet.core.utils.AndroidImplicitConversions._

class NameDongleFragment extends BaseFragment with ContractFragment[CreateDonglePairingActivity.CreateDonglePairingProccessContract] {

  lazy val nameEditText = TR(R.id.name).as[EditText]
  lazy val bottomText = TR(R.id.bottom_text).as[TextView]
  lazy val frame = TR(R.id.frame).as[View]

  override def onCreate(b: Bundle): Unit = {
    super.onCreate(b)
    setHasOptionsMenu(true)
  }

  override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
    inflater.inflate(R.layout.name_dongle_fragment, container, false)
  }


  override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
    super.onViewCreated(view, savedInstanceState)
    nameEditText.setOnEditorActionListener((actionId: Int, event: KeyEvent) => {
      actionId match {
        case EditorInfo.IME_ACTION_DONE => nextStep()
        case _ => false
      }
    })
  }

  override def onCreateOptionsMenu(menu: Menu, inflater: MenuInflater): Unit = {
    super.onCreateOptionsMenu(menu, inflater)
    inflater.inflate(R.menu.done_menu, menu)
  }


  override def onOptionsItemSelected(item: MenuItem): Boolean = {
    super.onOptionsItemSelected(item)
    item.getItemId match {
      case R.id.action_done => nextStep()
      case _ => false
    }
  }

  override def onResume(): Unit = {
    super.onResume()
    activity.foreach((activity) => {
      activity.getSupportActionBar.setDisplayHomeAsUpEnabled(false)
      activity.getSupportActionBar.setHomeButtonEnabled(false)
    })
    frame.getViewTreeObserver.addOnGlobalLayoutListener(layoutObserver)
    nameEditText.requestFocus()
    nameEditText.setSelection(nameEditText.getText().length())
  }


  override def onPause(): Unit = {
    super.onPause()
    frame.getViewTreeObserver.removeOnGlobalLayoutListener(layoutObserver)
  }

  val layoutObserver = new OnGlobalLayoutListener {
    override def onGlobalLayout(): Unit = {
      if (frame.getHeight < Convert.dpToPx(200)) {
        bottomText.setVisibility(View.GONE)
      }
      else {
        bottomText.setVisibility(View.VISIBLE)
      }
    }
  }

  private def nextStep(): Boolean = {
    if (nameEditText.getText().length() > 0)
      contract.setDongleName(nameEditText.getText().toString)
    true
  }

  override def tag: String = "NameDongleFragment"
}