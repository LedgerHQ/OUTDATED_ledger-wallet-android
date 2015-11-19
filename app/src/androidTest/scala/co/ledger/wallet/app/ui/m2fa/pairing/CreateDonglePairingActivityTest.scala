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
package co.ledger.wallet.app.ui.m2fa.pairing

import java.util.concurrent.{TimeUnit, CountDownLatch}

import android.app.Instrumentation
import android.content.Context
import android.os.{Looper, Handler}
import android.test.ActivityInstrumentationTestCase2
import android.view.KeyEvent
import co.ledger.wallet.R
import co.ledger.wallet.app.Config
import co.ledger.wallet.core.crypto.ECKeyPair
import co.ledger.wallet.app.api.m2fa.{MockPairingApi, PairingApiServer}
import junit.framework.Assert
import org.spongycastle.util.encoders.Hex
import scala.concurrent._
import scala.concurrent.ExecutionContext.Implicits.global

class CreateDonglePairingActivityTest extends ActivityInstrumentationTestCase2[CreateDonglePairingActivity](classOf[CreateDonglePairingActivity]) {

  implicit var context: Context = _
  var server: PairingApiServer = _
  var activity: CreateDonglePairingActivity = _
  var instrumentation: Instrumentation = _

  override def setUp(): Unit = {
    super.setUp()
    setActivityInitialTouchMode(false)
    instrumentation = getInstrumentation
    activity = getActivity
    context = activity
  }

  def testShouldCompletePairing(): Unit = {
    val signal = new CountDownLatch(1)

    implicit val delayTime = 500L
    activity.pairingApi.keypair = ECKeyPair.create(Hex.decode("dbd39adafe3a007706e61a17e0c56849146cfe95849afef7ede15a43a1984491"))
    server = new PairingApiServer(500L) {
      override def onSendChallenge(s: String, send: (String) => Unit): Unit = {
        waitForFragment("PairingInProgressFragment_2") {
          super.onSendChallenge(s, send)
          waitForFragment("PairingChallengeFragment") {
            //activity.getSupportFragmentManager.findFragmentByTag("PairingChallengeFragment").asInstanceOf[PairingChallengeFragment].pinTextView.setText("2C05")
            Future {
              getInstrumentation.waitForIdleSync()
              getInstrumentation.sendStringSync("2C05")
              getInstrumentation.waitForIdleSync()
            }
          }
        }
      }

      override def onSendPairing(s: String, send: (String) => Unit): Unit = {
        waitForFragment("PairingInProgressFragment_4") {
          super.onSendPairing(s, send)
          waitForFragment("NameDongleFragment") {
            Future {
              getInstrumentation.waitForIdleSync()
              for (i <- 0 to 100) {
                getInstrumentation.sendKeyDownUpSync(KeyEvent.KEYCODE_DEL)
              }
              getInstrumentation.sendStringSync("My great test wallet")
              getInstrumentation.waitForIdleSync()
              activity.toolbar.getMenu.performIdentifierAction(R.id.action_done, 0)
            }
          }
        }
      }
    }
    server.run()

    activity.postResult = (resultCode) => {
      Assert.assertEquals("Pairing should be done", resultCode, CreateDonglePairingActivity.ResultOk)
      signal.countDown()
    }

    Assert.assertTrue("Current fragment should be scan", getActivity.getFragmentManager.findFragmentByTag("ScanPairingQrCodeFragment").isVisible)

    delay {
      activity.setPairingId("1Nro9WkpaKm9axmcfPVp79dAJU1Gx7VmMZ")
    }

    signal.await(10, TimeUnit.SECONDS)
  }

  def testShouldEndWithCancel(): Unit = {
    val signal = new CountDownLatch(1)

    implicit val delayTime = 500L

    server = new PairingApiServer(500L) {
      override def onSendChallenge(s: String, send: (String) => Unit): Unit = {
        waitForFragment("PairingInProgressFragment_2") {
          super.onSendChallenge(s, send)
          waitForFragment("PairingChallengeFragment") {
            CreateDonglePairingActivityTest.this.server.sendDisconnect()
            Future {
              getInstrumentation.waitForIdleSync()
              getInstrumentation.sendStringSync("aaaa")
            }
          }
        }
      }
    }
    server.run()

    activity.postResult = (resultCode) => {
      Assert.assertEquals("Pairing should failed due client cancel", resultCode, CreateDonglePairingActivity.ResultPairingCancelled)
      signal.countDown()
    }

    Assert.assertTrue("Current fragment should be scan", getActivity.getFragmentManager.findFragmentByTag("ScanPairingQrCodeFragment").isVisible)
    delay {
      activity.setPairingId("1Nro9WkpaKm9axmcfPVp79dAJU1Gx7VmMZ")
    }

    signal.await()
  }

  def testShouldEndWithNetworkIssue(): Unit = {
    val signal = new CountDownLatch(1)

    implicit val delayTime = 500L

    server = new PairingApiServer(500L) {
      override def onSendChallenge(s: String, send: (String) => Unit): Unit = {
        waitForFragment("PairingInProgressFragment_2") {
          super.onSendChallenge(s, send)
          waitForFragment("PairingChallengeFragment") {
            CreateDonglePairingActivityTest.this.server.disconnectClient()
            Future {
              getInstrumentation.waitForIdleSync()
              getInstrumentation.sendStringSync("aaaa")
            }
          }
        }
      }
    }
    server.run()

    activity.postResult = (resultCode) => {
      Assert.assertTrue(s"Pairing should failed due to network error $resultCode",
          resultCode == CreateDonglePairingActivity.ResultNetworkError ||
          resultCode == CreateDonglePairingActivity.ResultTimeout)
      signal.countDown()
    }

    Assert.assertTrue("Current fragment should be scan", getActivity.getFragmentManager.findFragmentByTag("ScanPairingQrCodeFragment").isVisible)
    delay {
      activity.setPairingId("1Nro9WkpaKm9axmcfPVp79dAJU1Gx7VmMZ")
    }

    signal.await()
  }

  override def tearDown(): Unit = {
    server.stop()
    super.tearDown()
  }

  def delay(r: => Unit)(implicit delayTime: Long): Unit = {
    new Handler(Looper.getMainLooper).postDelayed(new Runnable {
      override def run(): Unit = r
    }, delayTime)
  }

  def waitForFragment(fragmentTag: String, timeout: Long = 20000L)(r: => Unit): Unit = {
    implicit val delayTime = 200L
    val startTime = System.currentTimeMillis()
    val checkIfFragmentIsPresent = new (() => Unit)  {
      def apply():Unit = {
        val f = getActivity.getFragmentManager.findFragmentByTag(fragmentTag)
        if (f != null && f.isVisible)
          r
        else if (System.currentTimeMillis() - startTime >= timeout)
          Assert.fail("Timeout during waiting for fragment [" + fragmentTag + "]")
        else {
          delay {
           apply()
          }
        }
      }
    }
    checkIfFragmentIsPresent()
  }

}
