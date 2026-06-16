package com.github.premnirmal.ticker.news

import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote

/**
 * A single entry in the news feed: either a news [ArticleNewsFeed] or a [TrendingStockNewsFeed]
 * carousel of trending [Quote]s.
 *
 * Pure, platform-agnostic presentation model depending only on the already-shared
 * [NewsArticle]/[Quote], so it lives in `commonMain` (same `ticker.news` package as before) and is
 * ready for the shared news view models / Compose Multiplatform UI in later phases.
 */
sealed class NewsFeedItem {
    class ArticleNewsFeed(val article: NewsArticle) : NewsFeedItem()
    class TrendingStockNewsFeed(val quotes: List<Quote>) : NewsFeedItem()
}
