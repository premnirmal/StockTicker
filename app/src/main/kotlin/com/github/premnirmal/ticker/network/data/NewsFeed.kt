package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName

class NewsFeed {
  @SerializedName("status") var status: String? = ""
  @SerializedName("totalResults") var totalResults: Int = 0
  @SerializedName("articles") var articles: List<NewsArticle> = emptyList()
}