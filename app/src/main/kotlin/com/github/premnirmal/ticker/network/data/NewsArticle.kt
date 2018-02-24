package com.github.premnirmal.ticker.network.data

import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter

data class NewsArticle(var url: String? = "") {

  companion object {
    private val OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
  }

  var author: String? = null
  var title: String? = null
  var description: String? = null
  var urlToImage: String? = null
  var source: Source? = null
  var publishedAt: String? = null

  fun getSourceName(): String? = source?.name

  fun date(): LocalDateTime = LocalDateTime.parse(publishedAt,
      DateTimeFormatter.ISO_DATE_TIME)

  fun dateString(): String = OUTPUT_FORMATTER.format(date())
}