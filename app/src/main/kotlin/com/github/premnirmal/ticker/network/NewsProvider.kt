package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.NewsArticle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider {

  @Inject internal lateinit var newsApi: NewsApi

  init {
    Injector.appComponent.inject(this)
  }

  suspend fun getNews(query: String): List<NewsArticle> = withContext(Dispatchers.IO) {
    newsApi.getNewsFeed(query = query)
  }
}