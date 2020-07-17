package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.ChartApi
import com.github.premnirmal.ticker.network.data.DataPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject
import kotlin.math.absoluteValue

class HistoryProvider : IHistoryProvider {

  @Inject internal lateinit var chartApi: ChartApi

  private var cachedData: WeakReference<Pair<String, List<DataPoint>>>? = null

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun fetchDataShort(symbol: String): FetchResult<List<DataPoint>> = withContext(Dispatchers.IO) {
    val dataPoints =  try {
      if (symbol == cachedData?.get()?.first) {
        cachedData!!.get()!!.second.filter {
          it.getDate().isAfter(Range.TWO_WEEKS.end)
        }.toMutableList().sorted()
      } else {
        val fetchDataByRange = fetchDataByRange(symbol, Range.TWO_WEEKS)
        if (fetchDataByRange.wasSuccessful) {
          cachedData = WeakReference(Pair(symbol, fetchDataByRange.data))
          fetchDataByRange.data
        } else {
          return@withContext FetchResult.failure<List<DataPoint>>(
              FetchException("Error fetching datapoints", fetchDataByRange.error)
          )
        }
      }
    } catch (ex: Exception) {
      return@withContext FetchResult.failure<List<DataPoint>>(
          FetchException("Error fetching datapoints", ex)
      )
    }
    return@withContext FetchResult.success(dataPoints)
  }

  override suspend fun fetchDataByRange(
    symbol: String,
    range: Range
  ): FetchResult<List<DataPoint>> = withContext(Dispatchers.IO) {
    val dataPoints = try {
      val historicalData =
        chartApi.fetchChartData(
            symbol = symbol, interval = range.intervalParam(), range = range.rangeParam()
        )
      with(historicalData.chart.result.first()) {
        timestamp.mapIndexed { i, stamp ->
          val dataQuote = indicators.quote.first()
          DataPoint(
              stamp.toFloat(), dataQuote.high[i].toFloat(), dataQuote.low[i].toFloat(),
              dataQuote.open[i].toFloat(), dataQuote.close[i].toFloat()
          )
        }
      }.toMutableList().sorted()
    } catch (ex: Exception) {
      Timber.w(ex)
      return@withContext FetchResult.failure<List<DataPoint>>(
          FetchException("Error fetching datapoints", ex)
      )
    }
    return@withContext FetchResult.success(dataPoints)
  }

  private fun Range.intervalParam(): String {
    return when(this) {
      Range.ONE_DAY -> "1h"
      else -> "1d"
    }
  }

  private fun Range.rangeParam(): String {
    return "${duration.toDays().absoluteValue}d"
  }
}