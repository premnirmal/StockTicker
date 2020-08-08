package com.github.premnirmal.ticker.notifications

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import org.threeten.bp.LocalDate
import javax.inject.Inject

class DailySummaryNotificationWorker(
  context: Context,
  parameters: WorkerParameters
) : CoroutineWorker(context, parameters) {

  companion object {
    const val TAG = "DailySummaryNotificationWorker"
  }

  @Inject lateinit var notificationsHandler: NotificationsHandler
  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var appPreferences: AppPreferences

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun doWork(): Result {
    val today = LocalDate.now()
    return if (appPreferences.updateDays()
            .contains(today.dayOfWeek)
    ) {
      val fetch = stocksProvider.fetch()
      if (fetch.wasSuccessful) {
        notificationsHandler.notifyDailySummary()
        Result.success()
      } else {
        Result.retry()
      }
    } else {
      Result.success()
    }
  }
}