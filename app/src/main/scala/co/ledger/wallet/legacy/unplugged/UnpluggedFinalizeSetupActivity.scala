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
package co.ledger.wallet.legacy.unplugged

import android.content.Intent
import android.nfc.Tag
import android.os.Bundle
import android.view.{LayoutInflater, View, ViewGroup}
import android.widget.Toast
import co.ledger.wallet.core.base.BaseFragment
import co.ledger.wallet.common._
import co.ledger.wallet.dongle.NfcDongle
import co.ledger.wallet.nfc.Unplugged
import co.ledger.wallet.core.utils.{AndroidUtils, TR}
import co.ledger.wallet.core.utils.logs.Logger
import co.ledger.wallet.core.widget.TextView
import co.ledger.wallet.R
import scala.concurrent.ExecutionContext.Implicits.global

import scala.util.{Failure, Success}

class UnpluggedFinalizeSetupActivity extends UnpluggedSetupActivity {

  val TappingMode = UnpluggedFinalizeSetupActivity.TappingMode
  val LoadingMode = UnpluggedFinalizeSetupActivity.LoadingMode

  var mode = TappingMode

  private[this] var _unplugged: Option[NfcDongle] = None
  def unplugged = _unplugged

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    mode = TappingMode
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    setContentFragment(new UnpluggedFinalizeSetupActivity.TapFragment)
  }

  override protected def onTagDiscovered(tag: Tag): Unit = {
    if (mode == TappingMode)
      AndroidUtils.vibrate(150)
    super.onTagDiscovered(tag)
  }

  override def onPause(): Unit = {
    super.onPause()
    _unplugged = None
  }

  override protected def onUnpluggedDiscovered(unplugged: NfcDongle): Unit = {
    super.onUnpluggedDiscovered(unplugged)
    if (mode == TappingMode) {
      mode = LoadingMode
      _unplugged = Option(unplugged)
      setContentFragment(new UnpluggedFinalizeSetupActivity.LoadingFragment)
    }
  }

}

object UnpluggedFinalizeSetupActivity {

  val TappingMode = 0x01
  val LoadingMode = 0x02

  class TapFragment extends BaseFragment {
    override def onCreateView(inflater: LayoutInflater, container: ViewGroup, savedInstanceState: Bundle): View = {
      inflater.inflate(R.layout.unplugged_finalize_setup_activity, container, false)
    }

    override def onResume(): Unit = {
      super.onResume()
      getActivity.setTitle(R.string.unplugged_finalize_setup_activity_title)
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
      if (getActivity.asInstanceOf[UnpluggedFinalizeSetupActivity].isInCreationMode) {
        stepInstructionTextView.setText(R.string.unplugged_in_progress_step_text_creation)
      } else {
        stepInstructionTextView.setText(R.string.unplugged_in_progress_step_text_restore)
      }
    }

    override def onResume(): Unit = {
      super.onResume()
      val activity = getActivity.asInstanceOf[UnpluggedFinalizeSetupActivity]
      getActivity.setTitle(R.string.unplugged_in_progress_activity_title)
      if (activity.unplugged.isDefined) {
        val unplugged = activity.unplugged.get
        unplugged.setKeycard(activity.keycardSeed.get) flatMap {(_) =>
          activity.unplugged.get.setup(activity.pin.get, activity.mnemonicPhrase.get)
        } onComplete {
          case Success(_) =>
            postOnUiThread({
              val intent = new Intent(this, classOf[UnpluggedSetupCompleteActivity])
              intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
              getActivity.startActivity(intent)
              activity.finish()
              activity.startNextActivity(classOf[UnpluggedSetupCompleteActivity])
            })
          case Failure(error) =>
            error.printStackTrace()
            postOnUiThread({
              error.printStackTrace()
              Toast.makeText(getActivity, R.string.unplugged_tap_error_occured, Toast.LENGTH_LONG).show()
              activity.mode = TappingMode
              activity.setContentFragment(new TapFragment)
            })
        }
      } else {
        // We need to tap again
        activity.mode = TappingMode
        activity.setContentFragment(new TapFragment)
      }
    }
  }

}