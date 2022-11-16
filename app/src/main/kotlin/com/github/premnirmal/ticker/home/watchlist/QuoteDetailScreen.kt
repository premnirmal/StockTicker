package com.github.premnirmal.ticker.home.watchlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.ui.theme.AppCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
  quote: Quote,
  quoteDetailViewModel: QuoteDetailViewModel = hiltViewModel()
) {
  quoteDetailViewModel.loadQuote(quote.symbol)
  quoteDetailViewModel.fetchQuote(quote.symbol)
  quoteDetailViewModel.fetchNews(quote)
  quoteDetailViewModel.fetchQuoteInRealTime(quote.symbol)

  Scaffold(
      topBar = {
        TopAppBar(
            title = {
              Text(text = quote.symbol, style = MaterialTheme.typography.headlineMedium)
            }
        )
      }
  ) { paddingValues ->
    val details = quoteDetailViewModel.details.collectAsState(initial = emptyList())
    val quoteDetail = quoteDetailViewModel.quote.observeAsState()
    val articles: State<List<ArticleNewsFeed>?> =
      quoteDetailViewModel.newsData.observeAsState()
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        modifier = Modifier
            .fillMaxSize()
            .padding(paddingValues = paddingValues),
        contentPadding = PaddingValues(all = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
      item(span = {
        GridItemSpan(2)
      }) {
        Text(
            text = quote.name,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
      }
      item(span = {
        GridItemSpan(2)
      }) {
        Text(
            text = quote.priceFormat.format(quote.lastTradePrice),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
      }
      item(span = {
        GridItemSpan(2)
      }) {
        Row {
          Text(
              text = quote.changeStringWithSign(),
              color = quote.changeColour,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.End,
              modifier = Modifier.fillMaxWidth(0.5f)
          )
          Text(
              text = quote.changePercentStringWithSign(),
              color = quote.changeColour,
              style = MaterialTheme.typography.bodyMedium,
              textAlign = TextAlign.Start,
              modifier = Modifier.fillMaxWidth(0.5f)
          )
        }
      }
      items(count = details.value.size) { i ->
        val item = details.value[i]
        AppCard {
          Column(modifier = Modifier.fillMaxSize().padding(all = 8.dp)) {
            Text(
                text = stringResource(item.title),
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                text = item.data,
                style = MaterialTheme.typography.bodyMedium
            )
          }
        }
      }
      item(span = {
        GridItemSpan(2)
      }) {
        Text(
            text = stringResource(id = R.string.recent_news),
            style = MaterialTheme.typography.labelMedium
        )
      }
      items(
          count = articles.value?.size ?: 0,
          span = {
        GridItemSpan(2)
      }) { i ->
        val item = articles.value!![i]
        NewsCard(item.article)
      }
      if (quoteDetail.value?.wasSuccessful == true) {
        item(span = {
          GridItemSpan(2)
        }) {
          Text(
              text = quoteDetail.value?.data?.quoteSummary?.assetProfile?.longBusinessSummary
                  ?: "",
              style = MaterialTheme.typography.bodySmall
          )
        }
      }
    }
  }
}