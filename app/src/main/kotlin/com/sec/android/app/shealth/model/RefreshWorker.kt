package com.sec.android.app.shealth.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.isNetworkOnline
import javax.inject.Inject

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  companion object {
    const val TAG = "RefreshWorker"
    const val TAG_PERIODIC = "RefreshWorker_Periodic"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun doWork(): Result {
    return if (applicationContext.isNetworkOnline()) {
      val result = stocksProvider.fetch()
      if (result.hasError) {
        Result.failure()
      } else {
        Result.success()
      }
    } else {
      Result.retry()
    }
  }
}