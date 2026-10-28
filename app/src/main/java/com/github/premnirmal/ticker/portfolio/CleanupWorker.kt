package com.github.premnirmal.ticker.portfolio

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

class CleanupWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params), KoinComponent {

    companion object {
        const val TAG = "CleanupWorker"
        const val TAG_PERIODIC = "CleanupWorker_Periodic"
    }

    private val stocksProvider: StocksProvider by inject()

    private val widgetDataProvider: WidgetDataProvider by inject()

    override suspend fun doWork(): Result {
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
