package com.github.premnirmal.ticker.model

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.concurrency.ApplicationScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@RequiresApi(VERSION_CODES.LOLLIPOP)
class RefreshService : JobService() {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  override fun onCreate() {
    super.onCreate()
    Injector.appComponent.inject(this)
  }

  override fun onStartJob(params: JobParameters): Boolean {
    Timber.d("onStartJob ${params.jobId}")
    return if (isNetworkOnline()) {
      ApplicationScope.launch {
        stocksProvider.fetch()
        val needsReschedule = false
        jobFinished(params, needsReschedule)
      }
      // additional work is being performed
      true
    } else {
      stocksProvider.scheduleSoon()
      false
    }
  }

  override fun onStopJob(params: JobParameters): Boolean {
    Timber.d("onStopJob ${params.jobId}")
    // doesn't need reschedule
    return false
  }
}