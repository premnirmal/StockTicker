package com.github.premnirmal.ticker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.todayLocal
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class DailySummaryNotificationReceiver : BroadcastReceiver(), KoinComponent {

    private val notificationsHandler: NotificationsHandler by inject()

    private val appPreferences: AppPreferences by inject()

    private val clock: AppClock by inject()

    override fun onReceive(context: Context, intent: Intent?) {
        Timber.d("DailySummaryNotificationReceiver onReceive")
        val today = clock.todayLocal().toLocalDate()
        if (appPreferences.updateDays().contains(today.dayOfWeek.value)) {
            notificationsHandler.notifyDailySummary()
        }
    }
}
