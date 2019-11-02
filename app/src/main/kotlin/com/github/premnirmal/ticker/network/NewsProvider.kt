package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.NewsArticle
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider {

  @Inject internal lateinit var newsApi: NewsApi
  @Inject internal lateinit var clock: AppClock

  init {
    Injector.appComponent.inject(this)
  }

  fun getNews(query: String): Observable<List<NewsArticle>> {
    return Observable.fromCallable { runBlocking { newsApi.getNewsFeed(query = query) } }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }
}