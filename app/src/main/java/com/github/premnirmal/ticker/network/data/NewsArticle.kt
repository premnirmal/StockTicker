package com.github.premnirmal.ticker.network.data

import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.text.Html
import org.simpleframework.xml.Attribute
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.net.URL

@Root(name = "item", strict = false)
class NewsArticle : Comparable<NewsArticle> {

  companion object {
    private val OUTPUT_FORMATTER = DateTimeFormatter.ofPattern("MMM d")
  }

  @get:Element(name = "link")
  @set:Element(name = "link")
  var url: String = ""

  @get:Element(name = "title", required = false)
  @set:Element(name = "title", required = false)
  var title: String? = null

  @get:Element(name = "description", required = false)
  @set:Element(name = "description", required = false)
  var description: String? = null

  @get:Element(name = "pubDate", required = false)
  @set:Element(name = "pubDate", required = false)
  var publishedAt: String? = null

  @get:Element(name = "content", required = false)
  @set:Element(name = "content", required = false)
  var thumbnail: Thumbnail? = null

  val date: LocalDateTime by lazy {
    try {
      LocalDateTime.parse(publishedAt, DateTimeFormatter.RFC_1123_DATE_TIME)
    } catch (e: Exception) {
      Instant.parse(publishedAt)
          .atZone(ZoneId.systemDefault())
          .toLocalDateTime()
    }
  }

  val imageUrl: String?
    get() = thumbnail?.url

  fun dateString(): String = OUTPUT_FORMATTER.format(date)

  fun sourceName(): String {
    val url = URL(url)
    val host = url.host
    return host.orEmpty()
  }

  fun descriptionSanitized(): String {
    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
      Html.fromHtml(description.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
          .toString()
    } else {
      Html.fromHtml(description.orEmpty())
          .toString()
    }
  }

  fun titleSanitized(): String {
    return if (VERSION.SDK_INT >= VERSION_CODES.N) {
      Html.fromHtml(title.orEmpty(), Html.FROM_HTML_MODE_COMPACT)
          .toString()
    } else {
      Html.fromHtml(title.orEmpty())
          .toString()
    }
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false
    other as NewsArticle
    if (url != other.url) return false
    return true
  }

  override fun hashCode(): Int {
    return url.hashCode()
  }

  // Comparable<NewsArticle>

  override fun compareTo(other: NewsArticle): Int {
    return other.date.compareTo(this.date)
  }
}

@Root(name = "content", strict = false)
data class Thumbnail(
  @field:Attribute(name = "url", required = false)
  @param:Attribute(name = "url", required = false)
  val url: String? = null
)

fun NewsArticle(
  title: String,
  url: String,
  publishedAt: String,
  imageUrl: String? = null
) = NewsArticle().apply {
  this.title = title
  this.url = url
  this.publishedAt = publishedAt
  imageUrl?.let {
    this.thumbnail = Thumbnail(url = imageUrl)
  }
}
