package com.github.premnirmal.ticker.network.data

import nl.adaptivity.xmlutil.serialization.XmlElement
import nl.adaptivity.xmlutil.serialization.XmlSerialName
import kotlinx.serialization.Serializable

/**
 * Root RSS feed model. Migrated from the SimpleXML `@Root(name = "rss")` / `@Path("channel")` model
 * into `commonMain`; the inline `<item>` list lives under `<channel>`, and [articleList] preserves
 * the public accessor the previous model exposed so `NewsProvider` keeps working unchanged.
 */
@Serializable
@XmlSerialName("rss")
class NewsRssFeed(
    @XmlElement(true)
    val channel: Channel? = null
) {
    val articleList: List<NewsArticle>?
        get() = channel?.items
}

/**
 * RSS `<channel>` element holding the inline list of `<item>` articles.
 */
@Serializable
@XmlSerialName("channel")
class Channel(
    @XmlElement(true)
    @XmlSerialName("item")
    val items: List<NewsArticle> = emptyList()
)
