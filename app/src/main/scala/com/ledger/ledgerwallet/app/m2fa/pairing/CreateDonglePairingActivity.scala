/**
 *
 * CreateDonglePairingActivity
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
package com.ledger.ledgerwallet.app.m2fa.pairing

import java.util.concurrent.TimeoutException

import android.app.AlertDialog.Builder
import android.app.{AlertDialog, Activity}
import android.content.DialogInterface
import android.content.DialogInterface.OnClickListener
import android.os.Bundle
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.base.{BaseFragment, BaseActivity}
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.api.m2fa.{RequireDongleName, RequireChallengeResponse, RequirePairingId, PairingAPI}
import com.ledger.ledgerwallet.utils.TR
import com.ledger.ledgerwallet.widget.TextView
import scala.concurrent.Promise
import scala.util.{Try, Failure, Success}
import com.ledger.ledgerwallet.utils.AndroidImplicitConversions._

class CreateDonglePairingActivity extends BaseActivity with CreateDonglePairingActivity.CreateDonglePairingProccessContract {

  lazy val stepNumberTextView = TR(R.id.step_number).as[TextView]
  lazy val stepInstructionTextView = TR(R.id.instruction_text).as[TextView]
  private[this] var _pairingApi: PairingAPI = _
  def pairingApi = _pairingApi

  lazy val pairindId = Promise[String]()
  lazy val challengeResponse = Promise[String]()
  lazy val dongleName = Promise[String]()

  override def onCreate(savedInstanceState: Bundle): Unit = {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.create_dongle_pairing_activity)
    getSupportActionBar.setHomeButtonEnabled(true)
    getSupportActionBar.setDisplayHomeAsUpEnabled(true)
    gotToStep(1, TR(R.string.create_dongle_instruction_step_1).as[String], new ScanPairingQrCodeFragment())
  }

  override def onResume(): Unit = {
    super.onResume()
    if (pairingApi == null)
      pairingApi = new PairingAPI(this)
    if (pairingApi.future.isEmpty) {
      pairingApi.startPairingProcess()
      if (!getSupportFragmentManager.findFragmentById(R.id.fragment_container).isInstanceOf[ScanPairingQrCodeFragment]) {
        gotToStep(1, TR(R.string.create_dongle_instruction_step_1).as[String], new ScanPairingQrCodeFragment())
      }
    }
    pairingApi.future.get onComplete {
      case Success(pairedDongle) => postResult(CreateDonglePairingActivity.ResultOk)
      case Failure(ex) => {
        ex match {
          case disconnect: PairingAPI.ClientCancelledException => postResult(CreateDonglePairingActivity.ResultPairingCancelled)
          case wrongChallenge: PairingAPI.WrongChallengeAnswerException => postResult(CreateDonglePairingActivity.ResultWrongChallenge)
          case timeout: TimeoutException => postResult(CreateDonglePairingActivity.ResultTimeout)
          case e: InterruptedException =>
          case _ =>
            ex.printStackTrace()
            postResult(CreateDonglePairingActivity.ResultNetworkError)
        }
      }
    }
  }

  override def onPause(): Unit = {
    super.onPause()
    pairingApi.future.foreach (_.onComplete((_) => {}))
  }


  override def onDestroy(): Unit = {
    super.onDestroy()
    pairingApi.abortPairingProcess()
  }

  def pairingApi_=(api: PairingAPI): Unit = {
    _pairingApi = api
    _pairingApi onRequireUserInput {
      case RequirePairingId() => pairindId.future
      case RequireChallengeResponse(challenge) => {
        this runOnUiThread {
          gotToStep(3, TR(R.string.create_dongle_instruction_step_3).as[String], new PairingChallengeFragment(challenge))
        }
        challengeResponse.future
      }
      case RequireDongleName() => {
        this runOnUiThread {
          gotToStep(5, TR(R.string.create_dongle_instruction_step_5).as[String], new NameDongleFragment)
        }
        dongleName.future
      }
    }
  }

  
  override def gotToStep(stepNumber: Int, instructionText: CharSequence, fragment: BaseFragment): Unit = {
    stepNumberTextView.setText(stepNumber.toString + ".")
    stepInstructionTextView.setText(instructionText)
    val ft = getSupportFragmentManager.beginTransaction()
    if (stepNumber > 1)
      ft.setCustomAnimations(R.anim.slide_from_right, R.anim.slide_to_left, R.anim.slide_from_left, R.anim.slide_to_right)
    ft.replace(R.id.fragment_container, fragment, fragment.tag)
    ft.commitAllowingStateLoss()
  }

  override def setPairingId(id: String): Unit = {
    if (pairindId.isCompleted) return
    pairindId.success(id)
    gotToStep(2, TR(R.string.create_dongle_instruction_step_2).as[String],
      new PairingInProgressFragment(
        2,
        R.string.create_dongle_instruction_step_2_title,
        R.string.create_dongle_instruction_step_2_text
      )
    )
  }
  override def setDongleName(name: String): Unit = {
    val cleanName = name.trim
    if (!dongleName.isCompleted) {
      if (PairedDongle.all.exists(_.name.get == cleanName)) {
        new Builder(this)
          .setTitle(R.string.create_dongle_instruction_step_5_error_title)
          .setMessage(R.string.create_dongle_instruction_step_5_error_message)
          .setNegativeButton(android.R.string.ok, new OnClickListener {
          override def onClick(dialog: DialogInterface, which: Int): Unit = dialog.dismiss()
        }).create().show()
        return
      }
      dongleName.complete(Try(cleanName))
    }
  }

  override def setChallengeAnswer(answer: String): Unit = {
    if (challengeResponse.isCompleted) return
    challengeResponse.complete(Try(answer))
    gotToStep(4, TR(R.string.create_dongle_instruction_step_4).as[String],
      new PairingInProgressFragment(
        4,
        R.string.create_dongle_instruction_step_4_title,
        R.string.create_dongle_instruction_step_4_text
      )
    )
  }

  private var _postResult: (Int) => Unit = (resultCode) => {
    setResult(resultCode)
    finish()
  }

  def postResult = _postResult
  def postResult_=(postResult: (Int) => Unit) = {
    if (postResult == null)
      throw new IllegalArgumentException("Post result cannot be null")
    _postResult = postResult
  }

  postResult = (result: Int) => {
    setResult(result)
    finish()
  }

}

object CreateDonglePairingActivity {

  val CreateDonglePairingRequest = 0xCAFE

  val ResultOk = Activity.RESULT_OK
  val ResultCanceled = Activity.RESULT_CANCELED
  val ResultNetworkError = 0x02
  val ResultWrongChallenge = 0x03
  val ResultPairingCancelled = 0x04
  val ResultTimeout = 0x05

  trait CreateDonglePairingProccessContract {
    def gotToStep(stepNumber: Int, instructionText: CharSequence, fragment: BaseFragment): Unit

    def setPairingId(pairingId: String): Unit
    def setChallengeAnswer(answer: String): Unit
    def setDongleName(dongleName: String): Unit

  }

}
