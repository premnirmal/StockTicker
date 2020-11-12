package com.github.premnirmal.ticker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import org.threeten.bp.LocalDate
import timber.log.Timber
import javax.inject.Inject

class DailySummaryNotificationReceiver: BroadcastReceiver() {

  @Inject lateinit var notificationsHandler: NotificationsHandler
  @Inject lateinit var appPreferences: AppPreferences

  init {
    Injector.appComponent.inject(this)
  }

  override fun onReceive(context: Context, intent: Intent?) {
    Timber.d("DailySummaryNotificationReceiver onReceive")
    val today = LocalDate.now()
    if (appPreferences.updateDays().contains(today.dayOfWeek)) {
      notificationsHandler.notifyDailySummary()
    }
  }
}