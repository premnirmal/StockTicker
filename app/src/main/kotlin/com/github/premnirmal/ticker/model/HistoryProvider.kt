package com.github.premnirmal.ticker.model

import android.content.Context
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.QueryCreator
import com.github.premnirmal.ticker.network.data.historicaldata.History
import org.threeten.bp.ZonedDateTime
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.Collections
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton class HistoryProvider @Inject constructor() : IHistoryProvider {

  @Inject internal lateinit var stocksApi: StocksApi
  @Inject internal lateinit var context: Context

  init {
    Injector.inject(this)
  }

  override fun getHistory(ticker: String, range: Range): Observable<History> {
    val now = Tools.clock().todayZoned()
    val from: ZonedDateTime
    when (range) {
      Range.ONE_MONTH -> from = now.minusMonths(1)
      Range.THREE_MONTH -> from = now.minusMonths(3)
      else -> from = now.minusYears(1)
    }
    val query = QueryCreator.buildHistoricalDataQuery(ticker, from, now)
    return stocksApi.getHistory(query).map { historicalData ->
      Collections.sort(historicalData!!.query!!.mResult!!.quote)
      if (historicalData.query == null || historicalData.query!!.mResult == null) {
        History()
      } else {
        historicalData.query!!.mResult!!
      }
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
  }

  override fun getDataPoints(ticker: String,
      range: Range): Observable<Array<SerializableDataPoint?>> {
    return getHistory(ticker, range).map { history ->
      val dataPoints = arrayOfNulls<SerializableDataPoint?>(history.quote!!.size)
      for (i in history.quote!!.indices) {
        val quote = history!!.quote!![i]
        val point = SerializableDataPoint(quote.mClose.toFloat(), i, quote)
        dataPoints[i] = point
      }
      dataPoints
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
  }
}