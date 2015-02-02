/**
 *
 * CreateDonglePairingActivityTest
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 30/01/15.
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

import java.util.concurrent.CountDownLatch

import android.app.Instrumentation
import android.app.Instrumentation.ActivityMonitor
import android.content.Intent
import android.test.ActivityInstrumentationTestCase2
import com.ledger.ledgerwallet.app.{Config, TestConfig}
import com.ledger.ledgerwallet.remote.api.m2fa.PairingApiServer

class CreateDonglePairingActivityTest extends ActivityInstrumentationTestCase2[CreateDonglePairingActivity](classOf[CreateDonglePairingActivity]) {

  var server: PairingApiServer = _
  var activity: CreateDonglePairingActivity = _
  var instrumentation: Instrumentation = _

  override def setUp(): Unit = {
    super.setUp()
    Config.setImplementation(TestConfig)
    server = new PairingApiServer
    server.run()
    instrumentation = getInstrumentation
    activity = getActivity
  }

  def testShouldCompletePairing(): Unit = {
    val signal = new CountDownLatch(1)
    getInstrumentation.callActivityOnCreate(activity, null)
    val monitor = new ActivityMonitor()
    signal.await()
  }

  override def tearDown(): Unit = {
    server.stop()
    super.tearDown()
  }
}
