package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

data class NewsArticle(@SerializedName("url") var url: String? = "") {

  companion object {
    private val OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
  }

  @SerializedName("author") var author: String? = null
  @SerializedName("title") var title: String? = null
  @SerializedName("description") var description: String? = null
  @SerializedName("urlToImage") var urlToImage: String? = null
  @SerializedName("source") var newsSource: NewsSource? = null
  @SerializedName("publishedAt") var publishedAt: String? = null

  val sourceName: String
    get() = newsSource?.name.orEmpty()

  fun date(): LocalDateTime = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)

  fun dateString(): String = OUTPUT_FORMATTER.format(date())
}

data class NewsSource(@SerializedName("id") var id: String? = null) {
  @SerializedName("name") var name: String = ""
}