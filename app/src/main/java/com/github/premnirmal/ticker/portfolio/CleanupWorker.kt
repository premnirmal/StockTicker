package com.github.premnirmal.ticker.portfolio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import timber.log.Timber
import javax.inject.Inject

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    companion object {
        const val TAG = "CleanupWorker"
        const val TAG_PERIODIC = "CleanupWorker_Periodic"
    }

    @Inject internal lateinit var stocksProvider: StocksProvider
    @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

    override suspend fun doWork(): Result {
        Injector.appComponent().inject(this)
        val tickers = stocksProvider.tickers.value
        val toRemove = ArrayList<String>()
        for (ticker in tickers) {
            if (!widgetDataProvider.containsTicker(ticker)) {
                toRemove.add(ticker)
            }
        }
        stocksProvider.removeStocks(toRemove)
        stocksProvider.cleanup()
        Timber.d("Cleanup success")
        return Result.success()
    }
}