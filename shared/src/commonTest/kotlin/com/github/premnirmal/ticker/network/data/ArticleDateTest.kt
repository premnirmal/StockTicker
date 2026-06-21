package com.github.premnirmal.ticker.network.data

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [ArticleDate], the multiplatform RSS `pubDate` parser used to order news articles
 * and render their short date labels.
 */
class ArticleDateTest {

    @Test fun parsesRfc1123WithWeekday() {
        val date = ArticleDate.parse("Tue, 3 Jun 2008 11:05:30 GMT")
        assertEquals(6, date.month)
        assertEquals(3, date.day)
        assertTrue(date.isValid)
    }

    @Test fun parsesRfc1123WithoutWeekdayAndWithoutSeconds() {
        val date = ArticleDate.parse("3 Jun 2008 11:05 GMT")
        assertEquals(6, date.month)
        assertEquals(3, date.day)
        assertTrue(date.isValid)
    }

    @Test fun parsesIso8601Instant() {
        val date = ArticleDate.parse("2008-06-03T11:05:30Z")
        assertEquals(6, date.month)
        assertEquals(3, date.day)
        assertTrue(date.isValid)
    }

    @Test fun blankAndUnparseableReturnInvalid() {
        assertEquals(ArticleDate.INVALID, ArticleDate.parse(null))
        assertEquals(ArticleDate.INVALID, ArticleDate.parse("   "))
        assertEquals(ArticleDate.INVALID, ArticleDate.parse("not a date"))
        assertFalse(ArticleDate.INVALID.isValid)
    }

    @Test fun sortKeyOrdersNewerDatesHigher() {
        val older = ArticleDate.parse("Mon, 2 Jun 2008 11:05:30 GMT")
        val newer = ArticleDate.parse("Tue, 3 Jun 2008 11:05:30 GMT")
        assertTrue(newer.sortKey > older.sortKey)
    }

    @Test fun monthAbbreviationMapsValidMonths() {
        assertEquals("Jan", ArticleDate.monthAbbreviation(1))
        assertEquals("Dec", ArticleDate.monthAbbreviation(12))
        assertEquals("", ArticleDate.monthAbbreviation(0))
        assertEquals("", ArticleDate.monthAbbreviation(13))
    }
}
