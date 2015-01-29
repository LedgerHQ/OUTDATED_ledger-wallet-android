/**
 *
 * PairingAPI
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 28/01/15.
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
package com.ledger.ledgerwallet.remote.api.m2fa

import android.content.Context
import com.ledger.ledgerwallet.models.PairedDongle
import com.ledger.ledgerwallet.remote.HttpClient

import scala.concurrent.{Promise, Future}

class PairingAPI(context: Context, httpClient: HttpClient = HttpClient()) {

  private var _promise: Option[Promise[PairedDongle]] = None
  def future = _promise.map(_.future)

  private var _currentState = State.Resting
  private var _onRequireUserInput: (RequiredUserInput) => Future[String] = _

  def onRequireUserInput(f: (RequiredUserInput) => Future[String]): Unit =  {

    _onRequireUserInput = f
  }

  def startPairingProcess(): Future[PairedDongle] = {
    if (_onRequireUserInput == null) {
      throw new IllegalStateException("No user input provider are setup. " +
        "Please call onRequireUserInput with a valid provider before starting the pairing process")
    }
    if (_currentState != State.Resting) {
      throw new IllegalStateException("A pairing process is already in progress")
    }
    _promise = Some(Promise())
    _currentState = State.Starting

    _promise.get.future
  }

  def abortPairingProcess(): Unit = {
    if (_promise.isDefined) {
      _currentState = State.Resting
      _promise.get.failure(new InterruptedException())
      _promise = None
    }
  }

  def isStarted = _currentState != State.Resting

  private object State extends Enumeration {
    type State = Value
    val Resting, Starting, Connecting, Challenging, Naming = Value
  }

}

abstract class RequiredUserInput
case class RequirePairingId() extends RequiredUserInput
case class RequireChallengeResponse(challenge: String) extends RequiredUserInput
case class RequireDongleName() extends RequiredUserInput