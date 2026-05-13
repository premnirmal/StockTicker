package com.github.premnirmal.ticker.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.isNetworkOnline
import timber.log.Timber
import javax.inject.Inject

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "RefreshWorker"
        const val TAG_PERIODIC = "RefreshWorker_Periodic"
    }

    @Inject internal lateinit var stocksProvider: StocksProvider

    @Inject internal lateinit var alarmScheduler: AlarmScheduler

    @Inject internal lateinit var fetchEventLogger: FetchEventLogger

    override suspend fun doWork(): Result {
        Injector.appComponent().inject(this)
        return if (applicationContext.isNetworkOnline()) {
            if (!alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()) {
                Timber.i("RefreshWorker skipped: outside configured update window")
                fetchEventLogger.log(
                    source = "RefreshWorker",
                    event = "skipped_window",
                    detail = "outside configured update window"
                )
                return Result.success()
            }
            val result = stocksProvider.fetch()
            if (result.hasError) {
                Timber.w(result.error, "RefreshWorker fetch failed, requesting retry")
                fetchEventLogger.log(
                    source = "RefreshWorker",
                    event = "fetch_failed",
                    detail = result.error.message.orEmpty()
                )
                Result.retry()
            } else {
                Timber.i("RefreshWorker fetch succeeded")
                fetchEventLogger.log(
                    source = "RefreshWorker",
                    event = "fetch_success",
                    detail = "fetch completed"
                )
                Result.success()
            }
        } else {
            Timber.w("RefreshWorker skipped: no validated network, requesting retry")
            fetchEventLogger.log(
                source = "RefreshWorker",
                event = "no_network",
                detail = "retry requested"
            )
            Result.retry()
        }
    }
}
