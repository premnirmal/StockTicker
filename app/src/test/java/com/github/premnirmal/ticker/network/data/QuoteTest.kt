package com.github.premnirmal.ticker.network.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class QuoteTest {

    @Test
    fun matchesNewsArticleByCompanyName() {
        val quote = Quote(symbol = "NET", name = "Cloudflare Inc.")
        val article = NewsArticle(
            title = "Cloudflare expands its developer platform",
            url = "https://example.com/cloudflare",
            publishedAt = "Tue, 01 Jan 2026 00:00:00 GMT"
        )

        assertTrue(quote.matchesNewsArticle(article))
    }

    @Test
    fun matchesNewsArticleByUppercaseTickerOnly() {
        val quote = Quote(symbol = "NET", name = "Cloudflare Inc.")
        val article = NewsArticle(
            title = "NET shares rise after earnings",
            url = "https://example.com/net",
            publishedAt = "Tue, 01 Jan 2026 00:00:00 GMT"
        )

        assertTrue(quote.matchesNewsArticle(article))
    }

    @Test
    fun doesNotMatchLowercaseCommonWordTicker() {
        val quote = Quote(symbol = "NET", name = "Cloudflare Inc.")
        val article = NewsArticle(
            title = "Company reports higher net income",
            url = "https://example.com/net-income",
            publishedAt = "Tue, 01 Jan 2026 00:00:00 GMT"
        )

        assertFalse(quote.matchesNewsArticle(article))
    }

    @Test
    fun doesNotMatchTickerInsideAnotherWord() {
        val quote = Quote(symbol = "NET", name = "Cloudflare Inc.")
        val article = NewsArticle(
            title = "Internet providers face new rules",
            url = "https://example.com/internet",
            publishedAt = "Tue, 01 Jan 2026 00:00:00 GMT"
        )

        assertFalse(quote.matchesNewsArticle(article))
    }
}
