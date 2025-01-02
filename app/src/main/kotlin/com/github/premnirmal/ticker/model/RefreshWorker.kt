package com.github.premnirmal.ticker.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import javax.inject.Inject

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  companion object {
    const val TAG = "RefreshWorker"
    const val TAG_PERIODIC = "RefreshWorker_Periodic"
  }

  @Inject internal lateinit var stocksProvider: StocksProvider
  @Inject internal lateinit var alarmScheduler: AlarmScheduler

  override suspend fun doWork(): Result {
    return if (applicationContext.isNetworkOnline()) {
      Injector.appComponent().inject(this)
      if (!alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()) {
        return Result.success()
      }
      val result = stocksProvider.fetch()
      if (result.hasError) {
        Result.retry()
      } else {
        Result.success()
      }
    } else {
      Result.retry()
    }
  }
}