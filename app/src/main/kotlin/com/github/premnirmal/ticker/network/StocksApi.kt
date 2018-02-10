package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.ErrorBody
import com.github.premnirmal.ticker.network.data.Quote
import com.google.gson.Gson
import io.reactivex.Observable
import retrofit2.HttpException
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton
class StocksApi @Inject constructor() {

  @Inject lateinit internal var gson: Gson
  @Inject lateinit internal var financeApi: Robindahood
  @Inject lateinit internal var clock: AppClock

  var lastFetched: Long = 0

  init {
    Injector.appComponent.inject(this)
  }

  fun getStocks(tickerList: List<String>): Observable<List<Quote>> {
    val query = tickerList.joinToString(",")
    return financeApi.getStocks(query)
        .doOnNext {
          lastFetched = clock.currentTimeMillis()
        }
        .map { quoteNets ->
          val quotesMap = HashMap<String, Quote>()
          for ((symbol, name, lastTradePrice, changePercent, change, exchange, currency, description) in quoteNets) {
            val quote = Quote(symbol ?: "", name ?: "",
                lastTradePrice, changePercent, change, exchange ?: "",
                currency ?: "US", description ?: "")
            quotesMap.put(quote.symbol, quote)
          }
          quotesMap
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
            val errorBody: ErrorBody? = gson.fromJson(e.response().errorBody().string(),
                ErrorBody::class.java)
            errorBody?.let {
              val robindahoodException = RobindahoodException(it, e, e.code())
              Timber.w(robindahoodException)
              throw robindahoodException
            }
          }
        }
  }

}