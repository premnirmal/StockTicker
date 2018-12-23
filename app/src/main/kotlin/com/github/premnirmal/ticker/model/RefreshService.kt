package com.github.premnirmal.ticker.model

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.isNetworkOnline
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.Quote
import timber.log.Timber
import javax.inject.Inject

@RequiresApi(VERSION_CODES.LOLLIPOP)
class RefreshService : JobService() {

  @Inject
  internal lateinit var stocksProvider: IStocksProvider

  override fun onCreate() {
    super.onCreate()
    Injector.appComponent.inject(this)
  }

  override fun onStartJob(params: JobParameters): Boolean {
    Timber.i("onStartJob " + params.jobId)
    if (isNetworkOnline()) {
      stocksProvider.fetch().subscribe(
          object : SimpleSubscriber<List<Quote>>() {
            override fun onError(e: Throwable) {
              // StocksProvider will handle rescheduling the job
              val needsReschedule = false
              jobFinished(params, needsReschedule)
            }

            override fun onComplete() {
              // doesn't need reschedule
              val needsReschedule = false
              jobFinished(params, needsReschedule)
            }
          })
      // additional work is being performed
      return true
    } else {
      stocksProvider.scheduleSoon()
      return false
    }
  }

  override fun onStopJob(params: JobParameters): Boolean {
    Timber.i("onStopJob " + params.jobId)
    // doesn't need reschedule
    return false
  }
}