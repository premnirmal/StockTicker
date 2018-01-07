package com.github.premnirmal.ticker.network.data

import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.DateTimeFormatter
import java.util.Locale

data class NewsArticle(var url: String? = "") {

  companion object {
    val FORMATTER: DateTimeFormatter by lazy {
      DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss[XXX]", Locale.getDefault())
          .withZone(ZoneOffset.UTC)
    }
  }

  var author: String? = null
  var title: String? = null
  var description: String? = null
  var urlToImage: String? = null
  var publishedAt: String? = null
  var source: Source? = null

  fun getPublishedAt(): ZonedDateTime = ZonedDateTime.from(FORMATTER.parse(publishedAt))

  fun getSourceName(): String? = source?.name
}