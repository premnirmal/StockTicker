package com.sec.android.app.shealth.news

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.model.FetchResult
import com.sec.android.app.shealth.network.NewsProvider
import com.sec.android.app.shealth.network.data.NewsArticle
import kotlinx.coroutines.launch
import javax.inject.Inject

class NewsFeedViewModel : ViewModel() {

  @Inject lateinit var newsProvider: NewsProvider

  init {
    Injector.appComponent.inject(this)
  }

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