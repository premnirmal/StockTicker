package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.FetchException
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider {

  @Inject internal lateinit var newsApi: NewsApi

  init {
    Injector.appComponent.inject(this)
  }

  suspend fun getNews(query: String): FetchResult<List<NewsArticle>> = withContext(Dispatchers.IO) {
    try {
      val newsFeed = newsApi.getNewsFeed(query = query)
      return@withContext FetchResult(_data = newsFeed)
    } catch (ex: Exception) {
      if (ex is HttpException) {
        if (ex.code() == 401) {
          return@withContext FetchResult<List<NewsArticle>>(
              _error = FetchException("Error fetching news", ex),
              unauthorized = true)
        }
      }
      return@withContext FetchResult<List<NewsArticle>>(
          _error = FetchException("Error fetching news", ex))
    }
  }
}