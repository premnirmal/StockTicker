package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.NewsArticle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider @Inject constructor(
  private val coroutineScope: CoroutineScope,
  private val newsApi: NewsApi
) {

  private var cachedBusinessArticles: List<NewsArticle> = emptyList()

  fun initCache() {
    coroutineScope.launch { fetchMarketNews() }
  }

  suspend fun fetchNewsForQuery(query: String): FetchResult<List<NewsArticle>> =
    withContext(Dispatchers.IO) {
      try {
        val newsFeed = newsApi.getNewsFeed(query = query)
        val articles = newsFeed.articleList?.sorted() ?: emptyList()
        return@withContext FetchResult.success(articles)
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure<List<NewsArticle>>(
            FetchException("Error fetching news", ex))
      }
    }

  suspend fun fetchMarketNews(useCache: Boolean = false): FetchResult<List<NewsArticle>> =
    withContext(Dispatchers.IO) {
      try {
        if (useCache && cachedBusinessArticles.isNotEmpty()) {
          return@withContext FetchResult.success(cachedBusinessArticles)
        }
        val marketNewsArticles = newsApi.getNewsFeed(query = "stock market").articleList.orEmpty()
        val businessNewsArticles = newsApi.getBusinessNews().articleList.orEmpty()
        val articles: Set<NewsArticle> = HashSet<NewsArticle>().apply {
          addAll(marketNewsArticles)
          addAll(businessNewsArticles)
        }
        val newsArticleList = articles.toList().sorted()
        cachedBusinessArticles = newsArticleList
        return@withContext FetchResult.success(newsArticleList)
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure<List<NewsArticle>>(
            FetchException("Error fetching news", ex))
      }
    }
}