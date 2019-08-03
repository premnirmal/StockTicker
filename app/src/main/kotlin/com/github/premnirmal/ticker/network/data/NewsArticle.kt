package com.github.premnirmal.ticker.network.data

import com.google.gson.annotations.SerializedName
import org.threeten.bp.LocalDateTime
import org.threeten.bp.format.DateTimeFormatter
import android.net.Proxy.getHost
import java.net.URL

data class NewsArticle(@SerializedName("url") var url: String? = "") {

  companion object {
    private val OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
  }

  @SerializedName("title") var title: String? = null
  @SerializedName("description") var description: String? = null
  @SerializedName("date") var publishedAt: String? = null

  fun date(): LocalDateTime = LocalDateTime.parse(publishedAt, DateTimeFormatter.ISO_DATE_TIME)

  fun dateString(): String = OUTPUT_FORMATTER.format(date())

  fun sourceName(): String {
    val url = URL(url)
    val host = url.host
    return host.orEmpty()
  }
}