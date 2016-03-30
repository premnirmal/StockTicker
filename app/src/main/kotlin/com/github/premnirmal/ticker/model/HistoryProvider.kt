package com.github.premnirmal.ticker.model

import android.accounts.NetworkErrorException
import android.content.Context
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.QueryCreator
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.historicaldata.HistoricalData
import com.github.premnirmal.ticker.network.historicaldata.History
import org.joda.time.DateTime
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.util.*

/**
 * Created by premnirmal on 2/28/16.
 */
class HistoryProvider(private val stocksApi: StocksApi, private val context: Context) : IHistoryProvider {

  override fun getHistory(ticker: String, range: Range): Observable<History> {
    val now = DateTime.now()
    val from: DateTime
    when (range) {
      Range.ONE_MONTH -> from = now.minusMonths(1)
      Range.THREE_MONTH -> from = now.minusMonths(3)
      else -> from = now.minusYears(1)
    }

    return Observable.create { subscriber ->
      subscriber.onStart()
      val query = QueryCreator.buildHistoricalDataQuery(ticker, from, now)
      stocksApi.getHistory(query).map { historicalData ->
        Collections.sort(historicalData.query.mResult.quote)
        historicalData
      }.subscribe(object : Subscriber<HistoricalData>() {
        override fun onCompleted() {
          subscriber.onCompleted()
        }

        override fun onError(throwable: Throwable) {
          subscriber.onError(throwable)
        }

        override fun onNext(response: HistoricalData) {
          subscriber.onNext(response.query.mResult)
        }
      })
    }
  }

  override fun getDataPoints(ticker: String,
      range: Range): Observable<Array<SerializableDataPoint?>> {
    return Observable.create { subscriber ->
      subscriber.onStart()
      if (Tools.isNetworkOnline(context)) {
        getHistory(ticker, range).map { history ->
          val dataPoints = arrayOfNulls<SerializableDataPoint?>(history.quote.size)
          for (i in history.quote.indices) {
            val quote = history.quote[i]
            val point = SerializableDataPoint(quote.mClose.toFloat(), i, quote)
            dataPoints[i] = point
          }

          dataPoints
        }.observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
            .subscribe(object : Subscriber<Array<SerializableDataPoint?>>() {
              override fun onCompleted() {
                subscriber.onCompleted()
              }

              override fun onError(throwable: Throwable) {
                subscriber.onError(throwable)
              }

              override fun onNext(dataPoints: Array<SerializableDataPoint?>) {
                subscriber.onNext(dataPoints)
              }
            })

      } else {
        subscriber.onError(NetworkErrorException())
      }
    }
  }
}