package com.github.premnirmal.ticker.network.data

import io.ktor.http.Url
import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlinx.serialization.Serializable

/**
 * A single RSS news item. Migrated from the Android-only SimpleXML model into `commonMain` so the
 * news feeds can be parsed by the shared Ktor/xmlutil networking layer.
 *
 * The XML mapping is expressed with `kotlinx.serialization` + xmlutil annotations (replacing the
 * SimpleXML `@Root`/`@Element` annotations). The Android-specific HTML sanitization now goes through
 * the [sanitizeHtml] `expect`/`actual`, and date parsing/formatting through the multiplatform
 * [ArticleDate] (replacing `java.time`).
 */
@Serializable
@XmlSerialName("item")
class NewsArticle(
    @XmlElement(true)
    @XmlSerialName("link")
    var url: String = "",

    @XmlElement(true)
    @XmlSerialName("title")
    var title: String? = null,

    @XmlElement(true)
    @XmlSerialName("description")
    var description: String? = null,

    @XmlElement(true)
    @XmlSerialName("pubDate")
    var publishedAt: String? = null,

    @XmlElement(true)
    @XmlSerialName("content", namespace = MEDIA_NAMESPACE)
    var thumbnail: Thumbnail? = null
) : Comparable<NewsArticle> {

    private val articleDate: ArticleDate by lazy { ArticleDate.parse(publishedAt) }

    val imageUrl: String?
        get() = thumbnail?.url

    fun dateString(): String =
        if (articleDate.isValid) "${ArticleDate.monthAbbreviation(articleDate.month)} ${articleDate.day}" else ""

    fun sourceName(): String =
        try {
            Url(url).host
        } catch (e: Exception) {
            ""
        }

    fun descriptionSanitized(): String = sanitizeHtml(description.orEmpty())

    fun titleSanitized(): String = sanitizeHtml(title.orEmpty())

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false
        other as NewsArticle
        if (url != other.url) return false
        return true
    }

    override fun hashCode(): Int = url.hashCode()

    // Newest first.
    override fun compareTo(other: NewsArticle): Int =
        other.articleDate.sortKey.compareTo(this.articleDate.sortKey)

    companion object {
        const val MEDIA_NAMESPACE = "http://search.yahoo.com/mrss/"
    }
}

/**
 * RSS `media:content` element (carries the article thumbnail URL as an attribute).
 */
@Serializable
@XmlSerialName("content", namespace = NewsArticle.MEDIA_NAMESPACE)
class Thumbnail(
    @XmlSerialName("url")
    val url: String? = null
)

/**
 * Convenience factory mirroring the previous `:app` helper, used by Compose previews/tests to build
 * an article without going through XML deserialization.
 */
@Suppress("FunctionNaming")
fun NewsArticle(
    title: String,
    url: String,
    publishedAt: String,
    imageUrl: String? = null
): NewsArticle = NewsArticle(
    url = url,
    title = title,
    publishedAt = publishedAt,
    thumbnail = imageUrl?.let { Thumbnail(url = it) }
)
