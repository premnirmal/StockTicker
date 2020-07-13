package com.github.premnirmal.ticker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.components.Injector
import javax.inject.Inject

class DailySummaryNotificationWorker(
  context: Context,
  parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

  companion object {
    const val TAG = "DailySummaryNotificationWorker"
  }

  @Inject lateinit var notificationsHandler: NotificationsHandler

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun doWork(): Result {
    notificationsHandler.notifyDailySummary()
    return Result.success()
  }
}