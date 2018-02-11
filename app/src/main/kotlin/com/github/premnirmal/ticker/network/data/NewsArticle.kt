package com.github.premnirmal.ticker.network.data

data class NewsArticle(var url: String? = "") {

  var author: String? = null
  var title: String? = null
  var description: String? = null
  var urlToImage: String? = null
  var source: Source? = null

  fun getSourceName(): String? = source?.name
}