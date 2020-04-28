package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider {

  companion object {
    const val NEWS_QUERY = "Finance"
  }

  @Inject internal lateinit var newsApi: NewsApi

  private var cachedArticles: List<NewsArticle>? = null

  init {
    Injector.appComponent.inject(this)
  }

  fun initCache() {
    GlobalScope.launch { fetchNews(NEWS_QUERY) }
  }

  suspend fun fetchNewsForQuery(query: String): FetchResult<List<NewsArticle>> {
    return fetchNews(query, false)
  }

  suspend fun fetchNews(query: String, useCache: Boolean = false): FetchResult<List<NewsArticle>> = withContext(Dispatchers.IO) {
    try {
      if (useCache && !cachedArticles.isNullOrEmpty()) {
        return@withContext FetchResult.success(cachedArticles!!)
      }
      val newsFeed = newsApi.getNewsFeed(query = query)
      val articles = newsFeed.articleList?.sortedByDescending { it.date } ?: emptyList()
      cachedArticles = articles
      return@withContext FetchResult.success(articles)
    } catch (ex: Exception) {
      Timber.w(ex)
      return@withContext FetchResult.failure<List<NewsArticle>>(
          FetchException("Error fetching news", ex))
    }
  }
}