package com.github.premnirmal.ticker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import timber.log.Timber
import javax.inject.Inject

class DailySummaryNotificationReceiver: BroadcastReceiver() {

  @Inject lateinit var notificationsHandler: NotificationsHandler
  @Inject lateinit var appPreferences: AppPreferences
  @Inject lateinit var clock: AppClock

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("DailySummaryNotificationReceiver onReceive")
    Injector.appComponent().inject(this)
    val today = clock.todayLocal().toLocalDate()
    if (appPreferences.updateDays().contains(today.dayOfWeek)) {
      notificationsHandler.notifyDailySummary()
    }
  }
}