package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.NewsRssFeed
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider {

  @Inject internal lateinit var newsApi: NewsApi

  private var cachedBusinessFeed: NewsRssFeed? = null

  init {
    Injector.appComponent.inject(this)
  }

  fun initCache() {
    GlobalScope.launch { fetchBusinessNews() }
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

  suspend fun fetchBusinessNews(useCache: Boolean = false): FetchResult<List<NewsArticle>> =
    withContext(Dispatchers.IO) {
      try {
        if (useCache && !cachedBusinessFeed?.articleList.isNullOrEmpty()) {
          return@withContext FetchResult.success(checkNotNull(cachedBusinessFeed?.articleList).sorted())
        }
        val newsFeed = newsApi.getBusinessNews()
        val articles = newsFeed.articleList?.sorted() ?: emptyList()
        cachedBusinessFeed = newsFeed
        return@withContext FetchResult.success(articles)
      } catch (ex: Exception) {
        Timber.w(ex)
        return@withContext FetchResult.failure<List<NewsArticle>>(
            FetchException("Error fetching news", ex))
      }
    }
}