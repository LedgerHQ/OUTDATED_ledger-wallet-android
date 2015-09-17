/**
 *
 * UnpluggedFinalizeSetupActivity
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 15/09/15.
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

import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.Toast
import com.ledger.ledgerwallet.base.BaseFragment
import com.ledger.ledgerwallet.common._
import com.ledger.ledgerwallet.nfc.Unplugged
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.TextView
import com.ledger.ledgerwallet.{R, common}
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

class UnpluggedFinalizeSetupActivity extends UnpluggedSetupActivity {

  val TappingMode = 0x01
  val LoadingMode = 0x02

  private[this] var _mode = TappingMode
  private[this] var _unplugged: Option[Unplugged] = None

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    _mode = TappingMode
    setContentFragment(new TapFragment)
  }

  override def onPause(): Unit = {
    super.onPause()
    _unplugged = None
  }

  override protected def onUnpluggedDiscovered(unplugged: Unplugged): Unit = {
    super.onUnpluggedDiscovered(unplugged)
    if (_mode == TappingMode) {
      _mode = LoadingMode
      _unplugged = Option(unplugged)
      setContentFragment(new LoadingFragment)
    }
  }

  class TapFragment extends BaseFragment {
    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_finalize_setup_activity, container, false)
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      val stepNumberTextView = TR(view, R.id.step_number).as[TextView]
      val stepInstructionTextView = TR(view, R.id.instruction_text).as[TextView]
      stepNumberTextView.setText(R.string.unplugged_finalize_setup_step_number)
      stepInstructionTextView.setText(R.string.unplugged_finalize_setup_step_text)
    }
  }

  class LoadingFragment extends BaseFragment {
    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_in_progress_activity, container, false)
    }

    override def onViewCreated(view: View, savedInstanceState: Bundle): Unit = {
      super.onViewCreated(view, savedInstanceState)
      val stepNumberTextView = TR(view, R.id.step_number).as[TextView]
      val stepInstructionTextView = TR(view, R.id.instruction_text).as[TextView]
      stepNumberTextView.setText(R.string.unplugged_in_progress_step_number)
      if (isInCreationMode) {
        stepInstructionTextView.setText(R.string.unplugged_in_progress_step_text_creation)
      } else {
        stepInstructionTextView.setText(R.string.unplugged_in_progress_step_text_restore)
      }
    }

    override def onResume(): Unit = {
      super.onResume()
      if (_unplugged.isDefined) {

        _unplugged.get.setKeycard(keycardSeed.get) andThen {
          case Success(_) => _unplugged.get.setup(pin.get, mnemonicPhrase.get)
        } onComplete {
          case Success(_) =>
            postOnUiThread(startNextActivity(classOf[UnpluggedSetupCompleteActivity]))
          case Failure(error) =>
            postOnUiThread({
              error.printStackTrace()
              Toast.makeText(getActivity, R.string.unplugged_tap_error_occured, Toast.LENGTH_LONG).show()
              _mode = TappingMode
              setContentFragment(new TapFragment)
            })
        }
      } else {
        // We need to tap again
        _mode = TappingMode
        setContentFragment(new TapFragment)
      }
    }
  }

}
