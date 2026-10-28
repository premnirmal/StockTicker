package com.github.premnirmal.ticker.model

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.isNetworkOnline
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        const val TAG = "RefreshWorker"
        const val TAG_PERIODIC = "RefreshWorker_Periodic"
    }

    private val stocksProvider: StocksProvider by inject()

    private val alarmScheduler: AlarmScheduler by inject()

    private val fetchEventLogger: FetchEventLogger by inject()

    override suspend fun doWork(): Result {
        return if (applicationContext.isNetworkOnline()) {
            if (!alarmScheduler.isCurrentTimeWithinScheduledUpdateTime()) {
                Timber.d("RefreshWorker skipped: outside configured update window")
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
                Timber.d("RefreshWorker fetch succeeded")
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
