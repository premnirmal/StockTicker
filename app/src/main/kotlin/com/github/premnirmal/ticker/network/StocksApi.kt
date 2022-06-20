package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
import com.github.premnirmal.ticker.network.data.YahooQuoteNet
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Singleton
class StocksApi {

  @Inject internal lateinit var gson: Gson
  @Inject internal lateinit var yahooFinance: YahooFinance
  @Inject internal lateinit var suggestionApi: SuggestionApi
  @Inject internal lateinit var clock: AppClock
  var lastFetched: Long = 0

  init {
    Injector.appComponent.inject(this)
  }

  suspend fun getSuggestions(query: String): FetchResult<List<SuggestionNet>> =
    withContext(Dispatchers.IO) {
      val suggestions = try {
        suggestionApi.getSuggestions(query).result
      } catch (e: Exception) {
        Timber.w(e)
        return@withContext FetchResult.failure(FetchException("Error fetching", e))
      }
      val suggestionList = suggestions?.let { ArrayList(it) } ?: ArrayList()
      return@withContext FetchResult.success<List<SuggestionNet>>(suggestionList)
    }

  suspend fun getStocks(tickerList: List<String>): FetchResult<List<Quote>> =
    withContext(Dispatchers.IO) {
      try {
        val quoteNets = getStocksYahoo(tickerList)
        lastFetched = clock.currentTimeMillis()
        return@withContext FetchResult.success(quoteNets.toQuoteMap().toOrderedList(tickerList))
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure(FetchException("Failed to fetch", ex))
      }
    }

  suspend fun getStock(ticker: String): FetchResult<Quote> =
    withContext(Dispatchers.IO) {
      try {
        val quoteNets = getStocksYahoo(listOf(ticker))
        return@withContext FetchResult.success(quoteNets.first().toQuote())
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure(FetchException("Failed to fetch $ticker", ex))
      }
    }

  private suspend fun getStocksYahoo(tickerList: List<String>) = withContext(Dispatchers.IO) {
    val query = tickerList.joinToString(",")
    val quoteNets = yahooFinance.getStocks(query).quoteResponse.result
    quoteNets
  }

  private fun List<YahooQuoteNet>.toQuoteMap(): MutableMap<String, Quote> {
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

  private fun YahooQuoteNet.toQuote(): Quote {
    val quote = Quote(
        symbol = this.symbol,
        name = (this.name ?: this.longName).orEmpty(),
        lastTradePrice = this.lastTradePrice,
        changeInPercent = this.changePercent,
        change = this.change
    )
    quote.stockExchange = this.exchange ?: ""
    quote.currencyCode = this.currency ?: "USD"
    quote.annualDividendRate = this.annualDividendRate
    quote.annualDividendYield = this.annualDividendYield
    quote.region = this.region
    quote.quoteType = this.quoteType
    quote.longName = this.longName
    quote.gmtOffSetMilliseconds = this.gmtOffSetMilliseconds
    quote.dayHigh = this.regularMarketDayHigh
    quote.dayLow = this.regularMarketDayLow
    quote.previousClose = this.regularMarketPreviousClose
    quote.open = this.regularMarketOpen
    quote.regularMarketVolume = this.regularMarketVolume
    quote.trailingPE = this.trailingPE
    quote.marketState = this.marketState
    quote.tradeable = this.tradeable
    quote.triggerable = this.triggerable
    quote.fiftyTwoWeekLowChange = this.fiftyTwoWeekLowChange
    quote.fiftyTwoWeekLowChangePercent = this.fiftyTwoWeekLowChangePercent
    quote.fiftyTwoWeekHighChange = this.fiftyTwoWeekHighChange
    quote.fiftyTwoWeekHighChangePercent = this.fiftyTwoWeekHighChangePercent
    quote.fiftyTwoWeekLow = this.fiftyTwoWeekLow
    quote.fiftyTwoWeekHigh = this.fiftyTwoWeekHigh
    quote.dividendDate = this.dividendDate?.times(1000)
    quote.earningsTimestamp = this.earningsTimestamp?.times(1000)
    quote.fiftyDayAverage = this.fiftyDayAverage
    quote.fiftyDayAverageChange = this.fiftyDayAverageChange
    quote.fiftyDayAverageChangePercent = this.fiftyDayAverageChangePercent
    quote.twoHundredDayAverage = this.twoHundredDayAverage
    quote.twoHundredDayAverageChange = this.twoHundredDayAverageChange
    quote.twoHundredDayAverageChangePercent = this.twoHundredDayAverageChangePercent
    quote.marketCap = this.marketCap
    return quote
  }
}
