package com.github.premnirmal.ticker.home

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.Builder
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.tickerwidget.R
import java.util.concurrent.atomic.AtomicInteger

object NotificationID {
  private val c: AtomicInteger = AtomicInteger(1)
  val Id: Int
    get() = c.incrementAndGet()
}

class NotificationFactory(
  val context: Context,
  title: String,
  text: String,
  symbol: String
) {
  private val notificationId: Int = NotificationID.Id

  val intent = Intent(context, QuoteDetailActivity::class.java).apply {
    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
    putExtra(QuoteDetailActivity.TICKER, symbol)
  }

  private val pendingIntent: PendingIntent? = TaskStackBuilder.create(context)
      .run {
        // Add the intent, which inflates the back stack
        addNextIntentWithParentStack(intent)
        // Get the PendingIntent containing the entire back stack
        getPendingIntent(notificationId, PendingIntent.FLAG_UPDATE_CURRENT)
      }

  private val notificationBuilder: Builder =
    Builder(context, AppPreferences.CHANNEL_ID)
        .setSmallIcon(R.mipmap.ic_splash)
        .setContentTitle(
            title
        )
        .setContentIntent(pendingIntent)
        .setStyle(
            NotificationCompat.BigTextStyle()
                .bigText(text)
        )
        .setDefaults(
            NotificationCompat.DEFAULT_SOUND or NotificationCompat.DEFAULT_VIBRATE
        )
        .setPriority(NotificationCompat.PRIORITY_MAX)
        .setAutoCancel(true)

  fun sendNotification() {
    with(NotificationManagerCompat.from(context)) {
      // NotificationId is a unique int for each notification.
      notify(notificationId, notificationBuilder.build())
    }
  }
}