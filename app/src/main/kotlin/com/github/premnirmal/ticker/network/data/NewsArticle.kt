package com.github.premnirmal.ticker.network.data

import android.text.Html
import org.simpleframework.xml.Element
import org.simpleframework.xml.Root
import org.threeten.bp.LocalDateTime
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
  @get:Element(name = "title")
  @set:Element(name = "title")
  var title: String? = null
  @get:Element(name = "description")
  @set:Element(name = "description")
  var description: String? = null
  @get:Element(name = "pubDate")
  @set:Element(name = "pubDate")
  var publishedAt: String? = null

  val date: LocalDateTime by lazy {
    LocalDateTime.parse(publishedAt, DateTimeFormatter.RFC_1123_DATE_TIME)
  }

  fun dateString(): String = OUTPUT_FORMATTER.format(date)

  fun sourceName(): String {
    val url = URL(url)
    val host = url.host
    return host.orEmpty()
  }

  fun descriptionSanitized(): String {
    return Html.fromHtml(description).toString()
  }

  fun titleSanitized(): String {
    return Html.fromHtml(title).toString()
  }

  // Comparable<NewsArticle>

  override fun compareTo(other: NewsArticle): Int {
    return other.date.compareTo(this.date)
  }
}
