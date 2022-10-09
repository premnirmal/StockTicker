package com.github.premnirmal.ticker.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.NewsFeedItem.TrendingStockNewsFeed
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
  private val newsProvider: NewsProvider
): ViewModel() {

  val newsFeed: LiveData<FetchResult<List<NewsFeedItem>>>
    get() = _newsFeed
  private val _newsFeed = MutableLiveData<FetchResult<List<NewsFeedItem>>>()

  fun fetchNews(forceRefresh: Boolean = false) {
    viewModelScope.launch {
      val news = newsProvider.fetchMarketNews(useCache = !forceRefresh)
      val trending = newsProvider.fetchTrendingStocks(useCache = !forceRefresh)
      if (news.wasSuccessful) {
        val data = ArrayList<NewsFeedItem>()
        withContext(Dispatchers.Default) {
          data.addAll(news.data.map { ArticleNewsFeed(it) })
          if (trending.wasSuccessful) {
            val taken = trending.data.take(6)
            data.add(0, TrendingStockNewsFeed(taken))
          }
        }
        _newsFeed.value = FetchResult.success(data)
      } else {
        _newsFeed.value = FetchResult.failure(news.error)
      }
    }
  }
}