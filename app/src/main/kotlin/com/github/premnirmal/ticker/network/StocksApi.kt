package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.QuoteNet
import com.github.premnirmal.ticker.network.data.Suggestions
import com.github.premnirmal.ticker.network.data.YahooQuoteNet
import com.google.gson.Gson
import io.reactivex.Observable
import io.reactivex.functions.Function
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton
class StocksApi {

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var robindahood: Robindahood
  @Inject internal lateinit var yahooFinance: YahooFinance
  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var clock: AppClock
  var lastFetched: Long = 0

  init {
    Injector.appComponent.inject(this)
  }

  fun getSuggestions(query: String): Observable<Suggestions> {
    return Observable.fromCallable {
      runBlocking {
        suggestionApi.getSuggestions(query)
      }
    }
  }

  /**
   * Prefer robindahood, fallback to yahoo finance.
   */
  fun getStocks(tickerList: List<String>): Observable<List<Quote>> {
    return Observable.fromCallable {
      runBlocking {
        val query = tickerList.joinToString(",")
        robindahood.getStocks(query)
      }
    }
        .doOnNext {
          lastFetched = clock.currentTimeMillis()
        }
        .map { quoteNets -> toMap(quoteNets) }
        // Try to keep original order of tickerList.
        .map { quotesMap ->
          toOrderedList(tickerList, quotesMap)
        }
        .onErrorResumeNext(Function { e ->
          if (e is HttpException) {
            return@Function getStocksYahoo(tickerList)
          }
          throw e
        })
  }

  private fun getStocksYahoo(tickerList: List<String>): Observable<List<Quote>> {
    return Observable.fromCallable {
      runBlocking {
        val query = tickerList.joinToString(",")
        yahooFinance.getStocks(query)
      }
    }
        .doOnNext {
          lastFetched = clock.currentTimeMillis()
        }
        .map { quotesResponse ->
          val quoteNets = quotesResponse.quoteResponse!!.result
          toMapYahoo(quoteNets)
        }
        .map { quotesMap ->
          toOrderedList(tickerList, quotesMap)
        }
  }

  private fun toMap(quoteNets: List<QuoteNet>): MutableMap<String, Quote> {
    val quotesMap = HashMap<String, Quote>()
    for (quoteNet in quoteNets) {
      val quote = Quote.fromQuoteNet(quoteNet)
      quotesMap[quote.symbol] = quote
    }
    return quotesMap
  }

  private fun toMapYahoo(quoteNets: List<YahooQuoteNet>): MutableMap<String, Quote> {
    val quotesMap = HashMap<String, Quote>()
    for (quoteNet in quoteNets) {
      val quote = Quote.fromQuoteNet(quoteNet)
      quotesMap[quote.symbol] = quote
    }
    return quotesMap
  }

  private fun toOrderedList(tickerList: List<String>,
                            quotesMap: MutableMap<String, Quote>): List<Quote> {
    val quotes = ArrayList<Quote>()
    tickerList.filter { quotesMap.containsKey(it) }
        .mapTo(quotes) { quotesMap[it]!! }
    return quotes
  }
}