package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.NewsFeed
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider @Inject constructor() {

  @Inject lateinit var newsApi: NewsApi
  private var apiKey = ""

  init {
    Injector.appComponent.inject(this)
  }

  fun getNews(query: String): Observable<NewsFeed> {
    return newsApi.getNewsFeed(apiKey, query)
  }
}