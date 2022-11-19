package com.github.premnirmal.ticker.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.NewsFeedItem.TrendingStockNewsFeed
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.ProgressState
import com.github.premnirmal.ticker.ui.TopBar

@Composable
fun NewsFeedScreen(
  modifier: Modifier = Modifier,
  onQuoteClick: (Quote) -> Unit,
  viewModel: NewsFeedViewModel = hiltViewModel()
) {
  viewModel.fetchNews()
  val newsFeed = viewModel.newsFeed.observeAsState()
  when (newsFeed.value?.wasSuccessful) {
    true -> {
      val newsFeedItems = newsFeed.value!!.data
      NewsFeedItems(modifier, newsFeedItems, onQuoteClick)
    }
    false -> {
      ErrorState(modifier = modifier, text = "Error fetching news")
    }
    else -> {
      ProgressState(modifier = modifier)
    }
  }
}

@Composable
private fun NewsFeedItems(
  modifier: Modifier = Modifier,
  newsFeedItems: List<NewsFeedItem>,
  onQuoteClick: (Quote) -> Unit
) {
  Column {
    TopBar(text = "Trending")
    LazyColumn(
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      items(
          count = newsFeedItems.size,
          key = { i ->
            val item = newsFeedItems[i]
            if (item is ArticleNewsFeed) {
              item.article.url
            } else {
              (item as TrendingStockNewsFeed).quotes.size
            }
          }
      ) { i ->
        val data = newsFeedItems[i]
        if (data is TrendingStockNewsFeed) {
          Column(
              modifier = Modifier.fillMaxWidth(),
              verticalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            val trending = data.quotes.withIndex()
                .groupBy { it.index / 3 }
                .map { it.value.map { it.value } }
            trending.forEach { group ->
              Row(
                  modifier = Modifier
                      .fillMaxWidth()
                      .height(intrinsicSize = IntrinsicSize.Max),
                  horizontalArrangement = Arrangement.spacedBy(8.dp)
              ) {
                group.forEach { quote ->
                  QuoteCard(
                      quote = quote,
                      modifier = Modifier
                          .weight(1f, true)
                          .fillMaxHeight(),
                      onClick = { onQuoteClick(quote) }
                  )
                }
              }
            }
          }
        } else if (data is ArticleNewsFeed) {
          NewsCard(data.article)
        }
      }
    }
  }
}

@Preview
@Composable
fun NewsFeedPreview() {
  NewsFeedItems(
      newsFeedItems = listOf(
          TrendingStockNewsFeed(
              listOf(
                  Quote("Goog", "Alphabet Inc"),
                  Quote("MSFT", "Microsoft Corporation"),
                  Quote("TWTR", "Twitter Inc"),
                  Quote("TSLA", "Tesla Inc"),
                  Quote("MSFT", "Microsoft Corporation"),
                  Quote("TWTR", "Twitter Inc")
              )
          )
      ),
      onQuoteClick = {}
  )
}