package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.HistoricalDataApi
import com.github.premnirmal.tickerwidget.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.LocalDate
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject

class HistoryProvider : IHistoryProvider {

  @Inject
  internal lateinit var historicalDataApi: HistoricalDataApi
  private val apiKey = Injector.appComponent.appContext().getString(R.string.alpha_vantage_api_key)

  init {
    Injector.appComponent.inject(this)
  }

  override fun getHistoricalData(symbol: String): Observable<List<DataPoint>> {
    return historicalDataApi.query(apiKey = apiKey, symbol = symbol).map {
      val points = ArrayList<DataPoint>()
      it.weeklyTimeSeries.forEach { k, v ->
        val epochDate = LocalDate.parse(k, DateTimeFormatter.ISO_LOCAL_DATE).toEpochDay()
        points.add(DataPoint(epochDate.toFloat(), v.close.toFloat()))
      }
      points.sort()
      points as List<DataPoint>
    }.subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}