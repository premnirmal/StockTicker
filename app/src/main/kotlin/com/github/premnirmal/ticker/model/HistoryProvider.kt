package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.HistoricalDataApi
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.lang.ref.WeakReference
import javax.inject.Inject

class HistoryProvider : IHistoryProvider {

  @Inject internal lateinit var historicalDataApi: HistoricalDataApi
  private val apiKey = Injector.appComponent.appContext()
      .getString(R.string.alpha_vantage_api_key)

  private var cachedData: WeakReference<Pair<String, List<DataPoint>>>? = null

  init {
    Injector.appComponent.inject(this)
  }

  override suspend fun getHistoricalDataShort(symbol: String): FetchResult<List<DataPoint>> {
    return withContext(Dispatchers.IO) {
      val dataPoints = try {
        val historicalData = historicalDataApi.getHistoricalData(apiKey = apiKey, symbol = symbol)
        val points = ArrayList<DataPoint>()
        historicalData.timeSeries.forEach { entry ->
          val epochDate = LocalDate.parse(entry.key, DateTimeFormatter.ISO_LOCAL_DATE)
              .toEpochDay()
          points.add(DataPoint(epochDate.toFloat(), entry.value.close.toFloat()))
        }
        points.sorted()
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure<List<DataPoint>>(FetchException("Error fetching datapoints", ex))
      }
      return@withContext FetchResult.success(dataPoints)
    }
  }

  override suspend fun getHistoricalDataByRange(
      symbol: String,
      range: Range
  ): FetchResult<List<DataPoint>> = withContext(Dispatchers.IO) {
    val dataPoints = try {
      if (symbol == cachedData?.get()?.first) {
        cachedData!!.get()!!.second.filter {
          it.getDate()
              .isAfter(range.end)
        }
            .toMutableList()
            .sorted()
      } else {
        cachedData = null
        val historicalData =
          historicalDataApi.getHistoricalDataFull(apiKey = apiKey, symbol = symbol)
        val points = ArrayList<DataPoint>()
        historicalData.timeSeries.forEach { entry ->
          val epochDate = LocalDate.parse(entry.key, DateTimeFormatter.ISO_LOCAL_DATE)
              .toEpochDay()
          points.add(DataPoint(epochDate.toFloat(), entry.value.close.toFloat()))
        }
        cachedData = WeakReference(Pair(symbol, points))
        points.filter {
          it.getDate()
              .isAfter(range.end)
        }
            .toMutableList()
            .sorted()
      }
    } catch (ex: Exception) {
      Timber.w(ex)
      return@withContext FetchResult.failure<List<DataPoint>>(FetchException("Error fetching datapoints", ex))
    }
    return@withContext FetchResult.success(dataPoints)
  }
}