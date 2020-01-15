package com.github.premnirmal.ticker.model

import android.app.job.JobParameters
import android.app.job.JobService
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

@RequiresApi(VERSION_CODES.LOLLIPOP)
class RefreshService : JobService(), CoroutineScope {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main

  override fun onCreate() {
    super.onCreate()
    Injector.appComponent.inject(this)
  }

  override fun onStartJob(params: JobParameters): Boolean {
    Timber.d("onStartJob ${params.jobId}")
    return if (isNetworkOnline()) {
      launch {
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