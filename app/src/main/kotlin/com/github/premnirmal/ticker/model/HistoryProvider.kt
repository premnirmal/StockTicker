package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IHistoryProvider.Range
import com.github.premnirmal.ticker.network.HistoricalDataApi
import com.github.premnirmal.ticker.network.data.DataPoint
import com.github.premnirmal.tickerwidget.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
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

  override fun getHistoricalDataShort(symbol: String): Observable<List<DataPoint>> {
    return historicalDataApi.getHistoricalData(apiKey = apiKey, symbol = symbol)
        .map {
          val points = ArrayList<DataPoint>()
          it.timeSeries.forEach { entry ->
            val epochDate = LocalDate.parse(entry.key, DateTimeFormatter.ISO_LOCAL_DATE)
                .toEpochDay()
            points.add(DataPoint(epochDate.toFloat(), entry.value.close.toFloat()))
          }
          points.sort()
          points as List<DataPoint>
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  override fun getHistoricalDataByRange(
    symbol: String,
    range: Range
  ): Observable<List<DataPoint>> {
    val observable: Observable<List<DataPoint>>
    if (symbol == cachedData?.get()?.first) {
      observable = Observable.fromCallable {
        val filtered = cachedData!!.get()!!.second.filter {
          it.getDate()
              .isAfter(range.end)
        }
            .toMutableList()
        filtered.sort()
        filtered
      }
    } else {
      cachedData = null
      observable = historicalDataApi.getHistoricalDataFull(apiKey = apiKey, symbol = symbol)
          .map {
            val points = ArrayList<DataPoint>()
            it.timeSeries.forEach { entry ->
              val epochDate = LocalDate.parse(entry.key, DateTimeFormatter.ISO_LOCAL_DATE)
                  .toEpochDay()
              points.add(DataPoint(epochDate.toFloat(), entry.value.close.toFloat()))
            }
            cachedData = WeakReference(Pair(symbol, points))
            val filtered = points.filter {
              it.getDate()
                  .isAfter(range.end)
            }
                .toMutableList()
            filtered.sort()
            filtered
          }
    }
    return observable.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}