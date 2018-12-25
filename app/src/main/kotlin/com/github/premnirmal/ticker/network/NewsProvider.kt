package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.NewsFeed
import com.github.premnirmal.tickerwidget.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import org.threeten.bp.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class NewsProvider @Inject constructor() {

  companion object {
    private val FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd")
  }

  @Inject internal lateinit var newsApi: NewsApi
  @Inject internal lateinit var clock: AppClock
  private val apiKey = Injector.appComponent.appContext().getString(R.string.news_api_key)

  init {
    Injector.appComponent.inject(this)
  }

  fun getNews(query: String): Observable<List<NewsArticle>> {
    val language = "en"//Locale.getDefault().language
    val from = clock.todayLocal().minusWeeks(1).format(FORMATTER)
    val to = clock.todayLocal().format(FORMATTER)
    return newsApi.getNewsFeed(apiKey = apiKey, query = query, language = language, from = from,
        to = to).map { feed: NewsFeed ->
      val articles = ArrayList<NewsArticle>()
      feed.articles?.let { result ->
        result.forEach { articles.add(it) }
      }
      articles.sortByDescending { it.date() }
      articles as List<NewsArticle>
    }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
  }
}