package com.github.premnirmal.ticker.home

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.github.premnirmal.ticker.AppPreferences

class NotificationChannelFactory(val context: Context) {
  init {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      val name = "Alerts"
      val descriptionText = "StockTicker Alerts"
      val importance = NotificationManager.IMPORTANCE_HIGH
      val channel = NotificationChannel(AppPreferences.CHANNEL_ID, name, importance).apply {
        description = descriptionText
        setShowBadge(true)
        lockscreenVisibility = Notification.VISIBILITY_PUBLIC
      }

      // Register the channel with the system
      val notificationManager = context.getSystemService(
          Context.NOTIFICATION_SERVICE
      ) as NotificationManager
      notificationManager.createNotificationChannel(channel)
    }
  }
}