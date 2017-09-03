package com.github.premnirmal.ticker.model

import android.annotation.TargetApi
import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi
import timber.log.Timber
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.SimpleSubscriber
import com.github.premnirmal.ticker.network.data.Quote
import javax.inject.Inject

@RequiresApi(VERSION_CODES.LOLLIPOP)
@TargetApi(VERSION_CODES.LOLLIPOP)
class RefreshService : JobService() {

  @Inject
  lateinit internal var stocksProvider: IStocksProvider

  override fun onCreate() {
    super.onCreate()
    Injector.appComponent.inject(this)
  }

  override fun onStartJob(params: JobParameters): Boolean {
    Timber.i("onStartJob " + params.jobId)
    stocksProvider.fetch().subscribe(
        object : SimpleSubscriber<List<Quote>>() {
          override fun onError(e: Throwable) {
            // needs reschedule
            val needsReschedule = true
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
  }

  override fun onStopJob(params: JobParameters): Boolean {
    Timber.i("onStopJob " + params.jobId)
    // doesn't need reschedule
    return false
  }
}