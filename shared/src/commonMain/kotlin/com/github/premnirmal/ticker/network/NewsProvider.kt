package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Aggregates the news/trending feeds (Google News, Yahoo Finance news, Yahoo "most active" and the
 * ApeWisdom fallback) into the shared [NewsArticle]/[Quote]/[FetchResult] domain model. Migrated
 * from the Android-only `:app` module into `commonMain`: it no longer depends on `Timber` (now
 * [AppLogger]), `Dispatchers.IO` (now [ioDispatcher]) or Hilt/`javax.inject` (constructed by the
 * platform DI layer). The public contract is unchanged so existing `:app` callers do not need to
 * change.
 */
class NewsProvider(
    private val coroutineScope: CoroutineScope,
    private val googleNewsApi: GoogleNewsApi,
    private val yahooNewsApi: YahooFinanceNewsApi,
    private val apeWisdom: ApeWisdom,
    private val yahooFinanceMostActive: YahooFinanceMostActiveApi,
    private val stocksApi: StocksApi
) {

    private var cachedBusinessArticles: List<NewsArticle> = emptyList()
    private var cachedTrendingStocks: List<Quote> = emptyList()

    fun initCache() {
        coroutineScope.launch {
            AppLogger.d("$TAG initCache: prefetching market news + trending stocks")
            fetchMarketNews()
            fetchTrendingStocks()
        }
    }

    suspend fun fetchNewsForQuery(query: String): FetchResult<List<NewsArticle>> =
        withContext(ioDispatcher) {
            try {
                val newsFeed = googleNewsApi.getNewsFeed(query = query)
                val articles = newsFeed.articleList?.sorted() ?: emptyList()
                return@withContext FetchResult.success(articles)
            } catch (ex: Exception) {
                AppLogger.w(ex)
                return@withContext FetchResult.failure<List<NewsArticle>>(
                    FetchException("Error fetching news", ex)
                )
            }
        }

    suspend fun fetchMarketNews(useCache: Boolean = false): FetchResult<List<NewsArticle>> =
        withContext(ioDispatcher) {
            try {
                if (useCache && cachedBusinessArticles.isNotEmpty()) {
                    return@withContext FetchResult.success(cachedBusinessArticles)
                }
                val marketNewsArticles = yahooNewsApi.getNewsFeed().articleList.orEmpty()
                val businessNewsArticles = googleNewsApi.getBusinessNews().articleList.orEmpty()
                val articles: Set<NewsArticle> = HashSet<NewsArticle>().apply {
                    addAll(marketNewsArticles)
                    addAll(businessNewsArticles)
                }
                val newsArticleList = articles.toList().sorted()
                cachedBusinessArticles = newsArticleList
                return@withContext FetchResult.success(newsArticleList)
            } catch (ex: Exception) {
                AppLogger.w(ex)
                return@withContext FetchResult.failure<List<NewsArticle>>(
                    FetchException("Error fetching news", ex)
                )
            }
        }

    suspend fun fetchTrendingStocks(useCache: Boolean = false): FetchResult<List<Quote>> =
        withContext(ioDispatcher) {
            try {
                if (useCache && cachedTrendingStocks.isNotEmpty()) {
                    return@withContext FetchResult.success(cachedTrendingStocks)
                }

                // adding this extra try/catch because html format can change and parsing will fail
                try {
                    val mostActive = yahooFinanceMostActive.getMostActive()
                    if (mostActive.isSuccessful) {
                        val symbols = mostActive.symbols
                        if (symbols.isNotEmpty()) {
                            val mostActiveStocks = stocksApi.getStocks(symbols)
                            if (mostActiveStocks.wasSuccessful && mostActiveStocks.data.isNotEmpty()) {
                                cachedTrendingStocks = mostActiveStocks.data
                                return@withContext mostActiveStocks
                            }
                            // Don't return here: fall through to the ApeWisdom fallback below. On the
                            // very first trending fetch (e.g. on a fresh launch) this quote request can
                            // fail because it is the first Yahoo call and the crumb/consent cookies have
                            // not been bootstrapped yet (loadCrumb only runs reactively on the quote's
                            // 401). Crucially, that first call can also come back HTTP 200 with an empty
                            // quote list — which `wasSuccessful` (data != null) still reports as a
                            // success — so we must also fall through when the data is empty, otherwise the
                            // empty list would be cached and the trending screen would stay blank until a
                            // manual retry. The fallback request reuses that now-established session and
                            // succeeds, populating the trending list on the first attempt.
                        }
                    }
                } catch (e: Exception) {
                    AppLogger.w(e, "$TAG fetchTrendingStocks: most-active path threw, falling back to ApeWisdom")
                }

                // fallback to apewisdom api
                val result = apeWisdom.getTrendingStocks().results
                val data = result.map { it.ticker }
                val trendingResult = stocksApi.getStocks(data)
                if (trendingResult.wasSuccessful) {
                    cachedTrendingStocks = trendingResult.data
                }
                return@withContext trendingResult
            } catch (ex: Exception) {
                AppLogger.w(ex, "$TAG fetchTrendingStocks failed")
                return@withContext FetchResult.failure<List<Quote>>(
                    FetchException("Error fetching trending", ex)
                )
            }
        }

    private companion object {
        private const val TAG = "[NewsProvider]"
    }
}
