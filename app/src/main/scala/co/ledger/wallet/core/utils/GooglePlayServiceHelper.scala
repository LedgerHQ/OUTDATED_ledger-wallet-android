/**
 *
 * GooglePlayServiceHelper
 * Ledger wallet
 *
 * Created by Pierre Pollastri on 11/02/15.
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
package co.ledger.wallet.core.utils

import android.app.Activity
import android.content.Context
import com.google.android.gms.common.{ConnectionResult, GooglePlayServicesUtil}
import com.google.android.gms.gcm.GoogleCloudMessaging
import scala.concurrent.ExecutionContext.Implicits.global

import scala.concurrent.{Promise, Future}

object GooglePlayServiceHelper {

  val PlayServicesResolutionRequest = 9000
  val GooglePlayServicePreferences = "GooglePlayServicePreferences"
  val GcmRegistrationIdPreferenceKey = "GcmRegistrationIdPreference"
  val GcmRegistrationVersionPreferenceKey = "GcmRegistrationVersionPreference"

  def isGooglePlayServicesAvailable(implicit context: Context): Boolean = GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SUCCESS
  def checkPlayServices(implicit context: Context): Boolean = {
    GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) match {
      case ConnectionResult.SUCCESS => true
      case error: Int => {
        if (GooglePlayServicesUtil.isUserRecoverableError(error) && context.isInstanceOf[Activity]) {
          GooglePlayServicesUtil.getErrorDialog(error, context.asInstanceOf[Activity], PlayServicesResolutionRequest).show()
        }
        false
      }
    }
  }

  def getGcmInstance(implicit context: Context): Option[GoogleCloudMessaging] = {
    if (checkPlayServices)
      Some(GoogleCloudMessaging.getInstance(context))
    else
      None
  }

  def getGcmRegistrationId(implicit context: Context): Future[RegistrationId] = {
    val registrationVersion = context
      .getSharedPreferences(GooglePlayServicePreferences, Context.MODE_PRIVATE)
      .getInt(GcmRegistrationVersionPreferenceKey, -1)
    if (registrationVersion != AndroidUtils.getAppVersion.getOrElse(-2)) {
      Future {
        val gcm = getGcmInstance.get
        val regId = gcm.register("1043077126300")
        context
          .getSharedPreferences(GooglePlayServicePreferences, Context.MODE_PRIVATE)
          .edit()
          .putString(GcmRegistrationIdPreferenceKey, regId)
          .putInt(GcmRegistrationVersionPreferenceKey, AndroidUtils.getAppVersion.getOrElse(0))
          .commit()
        inflateRegistrationId(isNew = true, context)
      }
    } else {
      val p = Promise[RegistrationId]()
      p.success(inflateRegistrationId(isNew = false, context))
      p.future
    }
  }

  private[this] def inflateRegistrationId(isNew: Boolean, context: Context): RegistrationId = {
    val preferences = context.getSharedPreferences(GooglePlayServicePreferences, Context.MODE_PRIVATE)
    val value = preferences.getString(GcmRegistrationIdPreferenceKey, null)
    new RegistrationId(value, isNew)
  }

 case class RegistrationId(value: String, isNew: Boolean)

}

