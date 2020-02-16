package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.IQuoteNet
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
import com.github.premnirmal.tickerwidget.BuildConfig
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton
class StocksApi {

  companion object {
    var DEBUG = BuildConfig.DEBUG
  }

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var robindahood: Robindahood
  @Inject internal lateinit var yahooFinance: YahooFinance
  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var clock: AppClock
  var lastFetched: Long = 0
  private var unauthorized = false

  init {
    Injector.appComponent.inject(this)
  }

  suspend fun getSuggestions(query: String): FetchResult<List<SuggestionNet>> =
    withContext(Dispatchers.IO) {
      val suggestions = try {
        suggestionApi.getSuggestions(query)
            .resultSet?.result
      } catch (e: Exception) {
        return@withContext FetchResult<List<SuggestionNet>>(
            _error = FetchException("Error fetching", e))
      }
      val suggestionList = suggestions?.let { ArrayList(it) } ?: ArrayList()
      if (suggestionList.isEmpty()) {
        suggestionList.add(0, SuggestionNet(query))
      }
      return@withContext FetchResult<List<SuggestionNet>>(_data = suggestionList)
    }

  /**
   * Prefer robindahood, fallback to yahoo finance.
   */
  suspend fun getStocks(tickerList: List<String>): FetchResult<List<Quote>> =
    withContext(Dispatchers.IO) {
      if (unauthorized) {
        return@withContext FetchResult<List<Quote>>(_unauthorized = true)
      }
      try {
        var quoteNets: List<IQuoteNet>
        try {
          val query = tickerList.joinToString(",")
          quoteNets = robindahood.getStocks(query)
        } catch (ex: HttpException) {
          unauthorized = ex.code() == 401 && !DEBUG
          if (unauthorized) {
            return@withContext FetchResult<List<Quote>>(_unauthorized = true)
          } else {
            quoteNets = getStocksYahoo(tickerList)
          }
        } catch (ex: Exception) {
          quoteNets = getStocksYahoo(tickerList)
        }
        lastFetched = clock.currentTimeMillis()
        return@withContext FetchResult(_data = quoteNets.toQuoteMap().toOrderedList(tickerList))
      } catch (ex: Exception) {
        return@withContext FetchResult<List<Quote>>(
            _error = FetchException("Failed to fetch", ex))
      }
    }

  suspend fun getStock(ticker: String): FetchResult<Quote> =
    withContext(Dispatchers.IO) {
      if (unauthorized) {
        return@withContext FetchResult<Quote>(_unauthorized = true)
      }
      try {
        var quoteNets: List<IQuoteNet>
        try {
          quoteNets = robindahood.getStocks(ticker)
        } catch (ex: HttpException) {
          unauthorized = ex.code() == 401 && !DEBUG
          if (unauthorized) {
            return@withContext FetchResult<Quote>(_unauthorized = true)
          } else {
            quoteNets = getStocksYahoo(listOf(ticker))
          }
        } catch (ex: Exception) {
          quoteNets = getStocksYahoo(listOf(ticker))
        }
        return@withContext FetchResult(_data = quoteNets.first().toQuote())
      } catch (ex: Exception) {
        return@withContext FetchResult<Quote>(
            _error = FetchException("Failed to fetch $ticker", ex))
      }
    }

  private suspend fun getStocksYahoo(tickerList: List<String>) = withContext(Dispatchers.IO) {
    val query = tickerList.joinToString(",")
    val quoteNets = yahooFinance.getStocks(query).quoteResponse!!.result
    quoteNets
  }

  private fun List<IQuoteNet>.toQuoteMap(): MutableMap<String, Quote> {
    val quotesMap = HashMap<String, Quote>()
    for (quoteNet in this) {
      val quote = quoteNet.toQuote()
      quotesMap[quote.symbol] = quote
    }
    return quotesMap
  }

  private fun MutableMap<String, Quote>.toOrderedList(tickerList: List<String>): List<Quote> {
    val quotes = ArrayList<Quote>()
    tickerList.filter { this.containsKey(it) }
        .mapTo(quotes) { this[it]!! }
    return quotes
  }

  private fun IQuoteNet.toQuote(): Quote {
    val quote = Quote(this.symbol ?: "")
    quote.name = this.name ?: ""
    quote.lastTradePrice = this.lastTradePrice
    quote.changeInPercent = this.changePercent
    quote.change = this.change
    quote.stockExchange = this.exchange ?: ""
    quote.currency = this.currency ?: "US"
    quote.description = this.description ?: ""
    return quote
  }
}
