package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.CrashLogger
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.data.ErrorBody
import com.github.premnirmal.ticker.network.data.Quote
import com.google.gson.Gson
import retrofit2.HttpException
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton class StocksApi @Inject constructor() {

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var financeApi: Robindahood

  var lastFetched: Long = 0

  init {
    Injector.inject(this)
  }

  fun getStocks(tickerList: List<String>): Observable<List<Quote>> {
    val query = tickerList.joinToString(",")
    return financeApi.getStocks(query)
        .map { quoteNets ->
          lastFetched = Tools.clock().currentTimeMillis()
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
            try {
              val errorBody: ErrorBody? = gson.fromJson(e.response().errorBody().string(), ErrorBody::class.java)
              if (errorBody != null) {
                val robindahoodException = RobindahoodException(errorBody, e, e.code())
                CrashLogger.logException(robindahoodException)
                throw robindahoodException
              }
            } catch (ex: Exception) {
              // ignored
            }
          }
        }
  }

}