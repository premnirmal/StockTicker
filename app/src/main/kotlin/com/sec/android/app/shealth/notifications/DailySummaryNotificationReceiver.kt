package com.sec.android.app.shealth.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.components.AppClock
import com.sec.android.app.shealth.components.Injector
import timber.log.Timber
import javax.inject.Inject

class DailySummaryNotificationReceiver: BroadcastReceiver() {

  @Inject lateinit var notificationsHandler: NotificationsHandler
  @Inject lateinit var appPreferences: AppPreferences
  @Inject lateinit var clock: AppClock

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("DailySummaryNotificationReceiver onReceive")
    Injector.appComponent.inject(this)
    val today = clock.todayLocal().toLocalDate()
    if (appPreferences.updateDays().contains(today.dayOfWeek)) {
      notificationsHandler.notifyDailySummary()
    }
  }
}