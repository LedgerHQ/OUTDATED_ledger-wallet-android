/**
 *
 * GcmIntentService
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
package com.ledger.ledgerwallet.app

import android.app.{PendingIntent, NotificationManager, IntentService}
import android.content.{Context, Intent}
import android.os.Bundle
import android.support.v4.app.NotificationCompat
import android.support.v4.content.WakefulBroadcastReceiver
import com.google.android.gms.gcm.GoogleCloudMessaging
import com.ledger.ledgerwallet.R
import com.ledger.ledgerwallet.utils.{AndroidUtils, TR}

class GcmIntentService extends IntentService("Ledger Wallet GCM Service") {

  implicit val context = this


  override def onHandleIntent(intent: Intent): Unit = {
    val extras = intent.getExtras
    val gcm = GoogleCloudMessaging.getInstance(this)
    if (!extras.isEmpty) {
      gcm.getMessageType(intent) match {
        case GoogleCloudMessaging.MESSAGE_TYPE_MESSAGE =>
          extras.getString("type") match {
            case "remote2fa" => postIncomingTransactionNotification(extras)
            case _ =>
          }
        case _ =>
      }
    }
    WakefulBroadcastReceiver.completeWakefulIntent(intent)
  }

  private[this] def postIncomingTransactionNotification(extras: Bundle): Unit = {
    if (AndroidUtils.isApplicationInForeground)
      return
    val manager = getSystemService(Context.NOTIFICATION_SERVICE).asInstanceOf[NotificationManager]
    val launchIntent = new Intent(this, classOf[HomeActivity])
    val intent = PendingIntent.getActivity(this, 0, launchIntent, 0)
    val notification = new NotificationCompat.Builder(this)
      .setSmallIcon(R.drawable.ic_notification)
      .setContentTitle(TR(R.string.app_name).as[String])
      .setContentText(extras.getString("message"))
      .setContentIntent(intent)
      .setAutoCancel(true)
      .setVibrate(Array[Long](0, 500, 100, 500))
      .build()
    manager.notify(GcmIntentService.IncomingTransactionNotificationId, notification)
  }

}

object GcmIntentService {

  val IncomingTransactionNotificationId = 1

}