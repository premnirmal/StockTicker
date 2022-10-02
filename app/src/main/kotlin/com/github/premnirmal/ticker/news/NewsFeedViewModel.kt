package com.github.premnirmal.ticker.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.NewsArticle
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NewsFeedViewModel @Inject constructor(
  private val newsProvider: NewsProvider
): ViewModel() {

  val newsFeed: LiveData<FetchResult<List<NewsArticle>>>
    get() = _newsFeed
  private val _newsFeed = MutableLiveData<FetchResult<List<NewsArticle>>>()

  fun fetchNews(forceRefresh: Boolean = false) {
    viewModelScope.launch {
      val result = newsProvider.fetchMarketNews(useCache = !forceRefresh)
      _newsFeed.value = result
    }
  }
}