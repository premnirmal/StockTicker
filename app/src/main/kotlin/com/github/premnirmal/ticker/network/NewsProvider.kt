package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.NewsFeed
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider @Inject constructor() {

  @Inject lateinit var newsApi: NewsApi
  private var apiKey = "2103c264597b466bb6c9b52cbbaef1f7"

  init {
    Injector.appComponent.inject(this)
  }

  fun getNews(query: String): Observable<List<NewsArticle>> {
    return newsApi.getNewsFeed(apiKey, query)
        .map { feed: NewsFeed ->
          val articles = ArrayList<NewsArticle>()
          feed.articles?.let { result ->
            result.forEach { articles.add(it) }
          }
          articles as List<NewsArticle>
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}