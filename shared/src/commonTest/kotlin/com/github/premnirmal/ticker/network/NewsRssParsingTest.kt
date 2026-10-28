package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.network.data.NewsArticle
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Parsing tests for the shared RSS news layer. These exercise [parseRssFeed] (xmlutil) against
 * representative Yahoo Finance and Google News documents, covering `media:content` thumbnails,
 * CDATA descriptions, RFC-1123 date parsing, source-host extraction, and newest-first sorting.
 */
class NewsRssParsingTest {

    private val yahooFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss xmlns:media="http://search.yahoo.com/mrss/" version="2.0">
          <channel>
            <title>Yahoo Finance</title>
            <link>https://finance.yahoo.com/news/</link>
            <item>
              <title>Markets rally on earnings</title>
              <link>https://finance.yahoo.com/news/markets-rally-123.html</link>
              <pubDate>Tue, 03 Jun 2008 11:05:30 +0000</pubDate>
              <media:content url="https://img.yahoo.com/a.jpg" type="image/jpeg" />
            </item>
            <item>
              <title>Tech stocks slip</title>
              <link>https://finance.yahoo.com/news/tech-stocks-456.html</link>
              <pubDate>Wed, 04 Jun 2008 09:00:00 +0000</pubDate>
              <media:content url="https://img.yahoo.com/b.jpg" type="image/jpeg" />
            </item>
          </channel>
        </rss>
    """.trimIndent()

    private val googleFeed = """
        <?xml version="1.0" encoding="UTF-8"?>
        <rss version="2.0">
          <channel>
            <title>Google News</title>
            <link>https://news.google.com/</link>
            <item>
              <title>Company posts record profit</title>
              <link>https://news.google.com/articles/company-profit</link>
              <pubDate>Mon, 02 Jun 2008 18:30:00 GMT</pubDate>
              <description><![CDATA[<a href="https://example.com">Read more</a> about it.]]></description>
            </item>
          </channel>
        </rss>
    """.trimIndent()

    @Test
    fun parsesYahooFeedWithThumbnailsAndSortsNewestFirst() {
        val feed = parseRssFeed(yahooFeed)
        val articles = assertNotNull(feed.articleList)
        assertEquals(2, articles.size)

        val first = articles.first { it.title == "Markets rally on earnings" }
        assertEquals("https://finance.yahoo.com/news/markets-rally-123.html", first.url)
        assertEquals("https://img.yahoo.com/a.jpg", first.imageUrl)
        assertEquals("finance.yahoo.com", first.sourceName())
        assertEquals("Jun 3", first.dateString())

        // Newest first: Jun 4 article should sort ahead of the Jun 3 article.
        val sorted = articles.sorted()
        assertEquals("Tech stocks slip", sorted.first().title)
    }

    @Test
    fun parsesGoogleFeedWithCdataDescriptionAndNoThumbnail() {
        val feed = parseRssFeed(googleFeed)
        val articles = assertNotNull(feed.articleList)
        assertEquals(1, articles.size)

        val article = articles.first()
        assertEquals("Company posts record profit", article.title)
        assertEquals("news.google.com", article.sourceName())
        assertEquals("Jun 2", article.dateString())
        assertEquals(null, article.imageUrl)
        // CDATA HTML is retained on the raw description field.
        assertTrue(article.description.orEmpty().contains("Read more"))
    }
}
