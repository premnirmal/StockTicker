package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.data.DataPoint
import kotlinx.coroutines.withContext

/**
 * Fetches a symbol's chart history for a [Range] and maps it into the shared [ChartData] model.
 * Moved from the Android-only `:app` module into `commonMain`: it no longer depends on `Timber`
 * (now [AppLogger]), `Dispatchers.IO` (now [ioDispatcher]) or Hilt/`javax.inject` (it is now plain
 * and constructed by the platform DI layer, e.g. `:app`'s `NetworkModule`). The public contract
 * (`suspend fun fetchDataByRange(symbol, range): FetchResult<ChartData>`) is unchanged so existing
 * `:app` callers do not need to change.
 */
class HistoryProvider(
    private val chartApi: ChartApi
) {

    suspend fun fetchDataByRange(
        symbol: String,
        range: Range
    ): FetchResult<ChartData> = withContext(ioDispatcher) {
        val chartData = try {
            val historicalData =
                chartApi.fetchChartData(
                    symbol = symbol,
                    interval = range.intervalParam(),
                    range = range.rangeParam()
                )
            with(historicalData.chart.result.first()) {
                val chartPreviousClose = meta.chartPreviousClose.toFloat()
                val regularMarketPrice = meta.regularMarketPrice.toFloat()
                val dataQuote = indicators?.quote?.firstOrNull()
                val highs = dataQuote?.high
                val lows = dataQuote?.low
                val opens = dataQuote?.open
                val closes = dataQuote?.close
                val dataPoints = timestamp?.mapIndexed { i, stamp ->
                    if (highs == null || lows == null || opens == null || closes == null ||
                        highs[i] === null || lows[i] === null ||
                        opens[i] === null || closes[i] === null
                    ) {
                        null
                    } else {
                        DataPoint(
                            stamp.toFloat(),
                            highs[i]!!.toFloat(),
                            lows[i]!!.toFloat(),
                            opens[i]!!.toFloat(),
                            closes[i]!!.toFloat()
                        )
                    }
                }?.filterNotNull()?.sorted().orEmpty()
                ChartData(
                    chartPreviousClose = chartPreviousClose,
                    regularMarketPrice = regularMarketPrice,
                    dataPoints = dataPoints
                )
            }
        } catch (ex: Exception) {
            AppLogger.w(ex)
            return@withContext FetchResult.failure(
                FetchException("Error fetching datapoints", ex)
            )
        }
        return@withContext FetchResult.success(chartData)
    }
}
