package com.github.premnirmal.ticker.model

import android.content.Context
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import javax.inject.Inject

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun doWork(): Result {
    if (applicationContext.isNetworkOnline()) {
      val result = stocksProvider.fetch()
      if (result.hasError) {
        return Result.retry()
      } else {
        return Result.success()
      }
    } else {
      stocksProvider.scheduleSoon()
      return Result.retry()
    }
  }
}