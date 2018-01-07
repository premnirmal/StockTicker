package com.github.premnirmal.ticker.network.data

data class NewsFeed(var status: String? = "") {

  var totalResults: Int = 0
  var articles: List<NewsArticle>? = null
}