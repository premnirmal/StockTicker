package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.network.data.ErrorBody
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.historicaldata.HistoricalData
import com.google.gson.Gson
import retrofit2.HttpException
import io.reactivex.Observable
import io.reactivex.functions.Action1
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton class StocksApi @Inject constructor() {

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var historyApi: YahooFinance
  @Inject internal lateinit var financeApi: Robindahood

  var lastFetched: Long = 0

  init {
    Injector.inject(this)
  }

  fun getHistory(query: String): Observable<HistoricalData> {
    return historyApi.getHistory(query)
  }

  fun getStocks(tickerList: List<String>): Observable<List<Quote>> {
    val query = tickerList.joinToString(",")
    return financeApi.getStocks(query)
        .map { quoteNets ->
          lastFetched = System.currentTimeMillis()
          quoteNets
        }
        .map { quoteNets ->
          StockConverter.convertQuoteNets(quoteNets)
        }
        // Try to keep original order of tickerList.
        .map { quotesMap ->
          val quotes = ArrayList<Quote>()
          tickerList
              .filter { quotesMap.containsKey(it) }
              .mapTo(quotes) { quotesMap.remove(it)!! }
          if (quotesMap.isNotEmpty()) {
            quotes.addAll(quotesMap.values)
          }
          quotes as List<Quote>
        }
        .doOnError { e ->
          if (e is HttpException) {
            val errorBody: ErrorBody? = gson.fromJson(e.response().errorBody().string(), ErrorBody::class.java)
            if (errorBody != null) {
              val robindahoodException = RobindahoodException(errorBody, e)
              CrashLogger.logException(robindahoodException)
              throw robindahoodException
            }
          }
        }
  }

}