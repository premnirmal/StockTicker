package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.IQuoteNet
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
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

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var robindahood: Robindahood
  @Inject internal lateinit var yahooFinance: YahooFinance
  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var clock: AppClock
  var lastFetched: Long = 0

  init {
    Injector.appComponent.inject(this)
  }

  suspend fun getSuggestions(query: String) = withContext(Dispatchers.IO) {
    val suggestions = try {
      suggestionApi.getSuggestions(query)
          .resultSet?.result
    } catch (e: Exception) {
      return@withContext FetchResult<List<SuggestionNet>>(_error = FetchException("Error fetching", e))
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
  suspend fun getStocks(tickerList: List<String>) = withContext(Dispatchers.IO) {
    val query = tickerList.joinToString(",")
    val quoteNets: List<IQuoteNet> = try {
      val stocks = robindahood.getStocks(query)
      lastFetched = clock.currentTimeMillis()
      stocks
    } catch (ex: HttpException) {
      getStocksYahoo(tickerList)
    }
    quoteNets.toQuoteMap()
        .toOrderedQuoteList(tickerList)
  }

  suspend fun getStock(ticker: String) = withContext(Dispatchers.IO) {
    val quoteNets = try {
      robindahood.getStocks(ticker)
    } catch (ex: HttpException) {
      getStocksYahoo(listOf(ticker))
    }
    return@withContext Quote.fromQuoteNet(quoteNets.first())
  }

  private suspend fun getStocksYahoo(tickerList: List<String>) = withContext(Dispatchers.IO) {
    val query = tickerList.joinToString(",")
    val quoteNets = yahooFinance.getStocks(query).quoteResponse!!.result
    lastFetched = clock.currentTimeMillis()
    quoteNets
  }

  private fun List<IQuoteNet>.toQuoteMap(): MutableMap<String, Quote> {
    val quotesMap = HashMap<String, Quote>()
    for (quoteNet in this) {
      val quote = Quote.fromQuoteNet(quoteNet)
      quotesMap[quote.symbol] = quote
    }
    return quotesMap
  }

  private fun MutableMap<String, Quote>.toOrderedQuoteList(tickerList: List<String>): List<Quote> {
    val quotes = ArrayList<Quote>()
    tickerList.filter { this.containsKey(it) }
        .mapTo(quotes) { this[it]!! }
    return quotes
  }
}