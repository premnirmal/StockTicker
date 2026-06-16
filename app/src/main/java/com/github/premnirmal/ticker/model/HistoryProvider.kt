package com.github.premnirmal.ticker.model

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class HistoryProvider @Inject constructor(
    private val chartApi: ChartApi
) {

    suspend fun fetchDataByRange(
        symbol: String,
        range: Range
    ): FetchResult<ChartData> = withContext(Dispatchers.IO) {
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
            Timber.w(ex)
            return@withContext FetchResult.failure(
                FetchException("Error fetching datapoints", ex)
            )
        }
        return@withContext FetchResult.success(chartData)
    }

    data class ChartData(
        val chartPreviousClose: Float,
        val regularMarketPrice: Float,
        val dataPoints: List<DataPoint>,
    ) {
        val change: Float
            get() = regularMarketPrice - chartPreviousClose

        val changeInPercent: Float
            get() = (regularMarketPrice - chartPreviousClose) / chartPreviousClose * 100f

        val isUp: Boolean
            get() = change > 0f

        val isDown: Boolean
            get() = change < 0f

        val changeColour: Color
            @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative

        fun changeString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(change)

        fun changeStringWithSign(): String {
            val changeString = AppPreferences.SELECTED_DECIMAL_FORMAT.format(change)
            if (change >= 0) {
                return "+$changeString"
            }
            return changeString
        }

        fun changePercentString(): String =
            "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"

        fun changePercentStringWithSign(): String {
            val changeString = "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"
            if (changeInPercent >= 0) {
                return "+$changeString"
            }
            return changeString
        }
    }
}
