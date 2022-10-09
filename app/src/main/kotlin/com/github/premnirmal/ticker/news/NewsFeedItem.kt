package com.github.premnirmal.ticker.news

import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote

sealed class NewsFeedItem {
  class ArticleNewsFeed(val article: NewsArticle) : NewsFeedItem()
  class TrendingStockNewsFeed(val quotes: List<Quote>) : NewsFeedItem()
}