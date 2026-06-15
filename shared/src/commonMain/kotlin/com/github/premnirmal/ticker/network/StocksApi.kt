package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.SuggestionsNet.SuggestionNet
import com.github.premnirmal.ticker.network.data.YahooQuoteNet
import kotlinx.coroutines.withContext

/**
 * Orchestrates the Yahoo Finance endpoints (quotes, crumb/cookie bootstrap, suggestions) and maps
 * their responses into the shared [Quote]/[FetchResult] domain model. Migrated from the Android-only
 * `:app` module into `commonMain`: it no longer depends on `Timber` (now [AppLogger]),
 * `Dispatchers.IO` (now [ioDispatcher]), `AppPreferences` (now the [CrumbStore] abstraction) or
 * Hilt/`javax.inject` (constructed by the platform DI layer). The public contract is unchanged so
 * existing `:app` callers do not need to change.
 *
 * Created by premnirmal on 3/3/16.
 */
class StocksApi(
    private val yahooFinanceInitialLoad: YahooFinanceInitialLoadApi,
    private val yahooFinanceCrumb: YahooCrumbApi,
    private val yahooFinance: YahooFinanceApi,
    private val crumbStore: CrumbStore,
    private val suggestionApi: SuggestionApi
) {

    val csrfTokenMatchPattern by lazy {
        Regex("csrfToken\" value=\"(.+)\">")
    }

    private suspend fun loadCrumb() {
        withContext(ioDispatcher) {
            try {
                val initialLoad = yahooFinanceInitialLoad.initialLoad()
                val html = initialLoad.html
                val url = initialLoad.url
                val match = csrfTokenMatchPattern.find(html)
                if (!match?.groupValues.isNullOrEmpty()) {
                    val csrfToken = match?.groupValues?.last().toString()
                    val sessionId = url.split("=").last()

                    val cookieConsent = yahooFinanceInitialLoad.cookieConsent(
                        url = url,
                        csrfToken = csrfToken,
                        sessionId = sessionId
                    )
                    if (!cookieConsent.isSuccessful) {
                        AppLogger.e("Failed cookie consent with code: ${cookieConsent.statusCode}")
                        return@withContext
                    }
                }

                val crumbResponse = yahooFinanceCrumb.getCrumb()
                if (crumbResponse.isSuccessful) {
                    val crumb = crumbResponse.crumb
                    if (!crumb.isNullOrEmpty()) {
                        crumbStore.setCrumb(crumb)
                    }
                } else {
                    AppLogger.e("Failed to get crumb with code: ${crumbResponse.statusCode}")
                }
            } catch (e: Exception) {
                AppLogger.e(e, "Crumb load failed")
            }
        }
    }

    suspend fun getSuggestions(query: String): FetchResult<List<SuggestionNet>> =
        withContext(ioDispatcher) {
            val suggestions = try {
                suggestionApi.getSuggestions(query).result
            } catch (e: Exception) {
                AppLogger.e(e)
                return@withContext FetchResult.failure(FetchException("Error fetching", e))
            }
            val suggestionList = suggestions?.let { ArrayList(it) } ?: ArrayList()
            return@withContext FetchResult.success<List<SuggestionNet>>(suggestionList)
        }

    suspend fun getStocks(tickerList: List<String>): FetchResult<List<Quote>> =
        withContext(ioDispatcher) {
            try {
                val quoteNets = getStocksYahoo(tickerList)
                    ?: return@withContext FetchResult.failure(FetchException("Failed to fetch"))
                return@withContext FetchResult.success(quoteNets.toQuoteMap().toOrderedList(tickerList))
            } catch (ex: Exception) {
                AppLogger.e(ex)
                return@withContext FetchResult.failure(FetchException("Failed to fetch", ex))
            }
        }

    suspend fun getStock(ticker: String): FetchResult<Quote> =
        withContext(ioDispatcher) {
            try {
                val quoteNets = getStocksYahoo(listOf(ticker))
                    ?: return@withContext FetchResult.failure(FetchException("Failed to fetch $ticker"))
                return@withContext FetchResult.success(quoteNets.first().toQuote())
            } catch (ex: Exception) {
                AppLogger.e(ex)
                return@withContext FetchResult.failure(FetchException("Failed to fetch $ticker", ex))
            }
        }

    private suspend fun getStocksYahoo(
        tickerList: List<String>,
        invocationCount: Int = 1
    ): List<YahooQuoteNet>? =
        withContext(ioDispatcher) {
            val query = tickerList.joinToString(",")
            var quotesResponse: YahooQuoteResult? = null
            try {
                quotesResponse = yahooFinance.getStocks(query)
                if (!quotesResponse.isSuccessful) {
                    AppLogger.e("Yahoo quote fetch failed with code ${quotesResponse.statusCode}")
                }
                if (quotesResponse.statusCode == 401) {
                    crumbStore.setCrumb(null)
                    loadCrumb()
                    if (invocationCount == 1) {
                        return@withContext getStocksYahoo(tickerList, invocationCount = invocationCount + 1)
                    }
                }
            } catch (ex: Exception) {
                AppLogger.e(ex)
                throw ex
            }
            val quoteNets = quotesResponse.response?.quoteResponse?.result
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
            lastTradePrice = this.lastTradePrice ?: 0f,
            changeInPercent = this.changePercent ?: 0f,
            change = this.change ?: 0f
        )
        quote.stockExchange = this.exchange ?: ""
        quote.currencyCode = this.currency ?: "USD"
        quote.annualDividendRate = this.annualDividendRate ?: 0f
        quote.annualDividendYield = this.annualDividendYield ?: 0f
        quote.region = this.region
        quote.quoteType = this.quoteType
        quote.longName = this.longName
        quote.gmtOffSetMilliseconds = this.gmtOffSetMilliseconds ?: 0L
        quote.dayHigh = this.regularMarketDayHigh
        quote.dayLow = this.regularMarketDayLow
        quote.previousClose = this.regularMarketPreviousClose ?: 0f
        quote.open = this.regularMarketOpen
        quote.regularMarketVolume = this.regularMarketVolume
        quote.trailingPE = this.trailingPE
        quote.marketState = this.marketState ?: ""
        quote.tradeable = this.tradeable ?: false
        quote.triggerable = this.triggerable ?: false
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
