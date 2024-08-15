package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells.Adaptive
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyGridScope
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.NewsFeedItem
import com.github.premnirmal.ticker.news.NewsFeedViewModel
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
  modifier: Modifier = Modifier,
  widthSizeClass: WindowWidthSizeClass,
  displayFeatures: List<DisplayFeature>,
  onQuoteClick: (Quote) -> Unit,
  searchViewModel: SearchViewModel = hiltViewModel(),
  newsViewModel: NewsFeedViewModel = hiltViewModel()
) {
  val onSuggestionClick: (Suggestion) -> Unit = {
    onQuoteClick(Quote(it.symbol))
  }
  var showAddRemoveForSuggestion by remember { mutableStateOf<Suggestion?>(null) }
  val onSuggestionAddRemoveClick: (Suggestion) -> Boolean = {
    showAddRemoveForSuggestion = it
    it.exists
  }
  val contentType: ContentType = calculateContentAndNavigationType(
      widthSizeClass = widthSizeClass, displayFeatures = displayFeatures
  ).second
  Scaffold(
      modifier = modifier
          .background(MaterialTheme.colorScheme.surface),
      topBar = {
        TopBar(text = stringResource(id = R.string.action_search))
      }
  ) { padding ->
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val results = searchViewModel.searchResult.observeAsState()
    val searchResults = remember { results }
    val trendingStocks = searchViewModel.fetchTrendingStocks()
        .observeAsState(emptyList())
    if (contentType == SINGLE_PANE) {
      LazyVerticalGrid(
          modifier = Modifier.padding(horizontal = 8.dp),
          columns = Adaptive(120.dp),
          contentPadding = padding,
          verticalArrangement = Arrangement.spacedBy(8.dp),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        searchAndTrending(
            searchQuery = searchQuery,
            trendingStocks = trendingStocks,
            searchResults = searchResults,
            onQuoteClick = onQuoteClick,
            onSuggestionClick = onSuggestionClick,
            onSuggestionAddRemoveClick = onSuggestionAddRemoveClick
        ) {
          searchQuery = it
          searchViewModel.fetchResults(searchQuery)
        }
        item(span = {
          GridItemSpan(maxLineSpan)
        }) {
          Spacer(modifier = Modifier.height(16.dp))
        }
      }
    } else {
      TwoPane(
          strategy = HorizontalTwoPaneStrategy(
              splitFraction = 1f / 2f,
          ),
          displayFeatures = displayFeatures,
          foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
          first = {
            LazyVerticalGrid(
                modifier = Modifier.padding(horizontal = 8.dp),
                columns = Adaptive(150.dp),
                contentPadding = padding,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
              searchAndTrending(
                  searchQuery = searchQuery,
                  trendingStocks = trendingStocks,
                  searchResults = searchResults,
                  onQuoteClick = onQuoteClick,
                  onSuggestionClick = onSuggestionClick,
                  onSuggestionAddRemoveClick = onSuggestionAddRemoveClick
              ) {
                searchQuery = it
                searchViewModel.fetchResults(searchQuery)
              }
              item(span = {
                GridItemSpan(maxLineSpan)
              }) {
                Spacer(modifier = Modifier.height(16.dp))
              }
            }
          },
          second = {
            newsViewModel.fetchNews()
            val feed = newsViewModel.newsFeed.observeAsState()
            val fetchResult: FetchResult<List<NewsFeedItem>>? = feed.value
            if (fetchResult != null) {
              val data = fetchResult.data
              if (data.isEmpty()) {
                ErrorState(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    text = stringResource(id = R.string.no_data)
                )
              } else {
                val news = data.filterIsInstance<NewsFeedItem.ArticleNewsFeed>()
                LazyColumn(
                    modifier = Modifier.padding(horizontal = 8.dp),
                    contentPadding = padding,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                  items(
                      count = news.size,
                      key = { i -> news[i].article.url }
                  ) { i ->
                    NewsCard(item = news[i].article)
                  }
                }
              }
            }
          }
      )
    }
  }
  showAddRemoveForSuggestion?.let { suggestion ->
    AddSuggestionScreen(
        suggestion = suggestion,
        onChange = { suggestion, widgetId ->
          suggestion.exists = !suggestion.exists
          if (suggestion.exists) {
            searchViewModel.addTickerToWidget(suggestion.symbol, widgetId)
          } else {
            searchViewModel.removeStock(suggestion.symbol, widgetId)
          }
          showAddRemoveForSuggestion = null
        },
        onDismissRequest = {
          showAddRemoveForSuggestion = null
        },
        widgetDataList = searchViewModel.getWidgetDataList()
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun LazyGridScope.searchAndTrending(
  searchQuery: String,
  trendingStocks: State<List<Quote>>,
  searchResults: State<FetchResult<List<Suggestion>>?>,
  onQuoteClick: (Quote) -> Unit,
  onSuggestionClick: (Suggestion) -> Unit,
  onSuggestionAddRemoveClick: (Suggestion) -> Boolean,
  onQueryChange: (String) -> Unit
) {
  var text = searchQuery
  item(span = {
    GridItemSpan(maxLineSpan)
  }) {
    TextField(
        value = text,
        onValueChange = {
          text = it
          onQueryChange(it)
        },
        label = { Text(stringResource(id = R.string.enter_a_symbol)) },
        singleLine = true,
        trailingIcon = {
          IconButton(
              enabled = text.isNotEmpty(),
              onClick = { onQueryChange("") }
          ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_close),
                contentDescription = null
            )
          }
        }
    )
  }
  if (searchResults.value != null) {
    if (searchResults.value?.wasSuccessful == true) {
      val suggestions = searchResults.value!!.data
      if (suggestions.isEmpty()) {
        trendingStocks(trendingStocks, onQuoteClick)
      } else {
        items(
            span = { GridItemSpan(maxLineSpan) },
            count = suggestions.size,
            key = { i -> suggestions[i].symbol }
        ) { i ->
          val suggestion = suggestions[i]
          SuggestionItem(
              suggestion = suggestion,
              onSuggestionClick = onSuggestionClick,
              onSuggestionAddRemoveClick = onSuggestionAddRemoveClick
          )
        }
      }
    } else {
      item(span = {
        GridItemSpan(maxLineSpan)
      }) {
        ErrorState(text = stringResource(R.string.error_fetching_suggestions))
      }
    }
  } else {
    trendingStocks(trendingStocks, onQuoteClick)
  }
}

private fun LazyGridScope.trendingStocks(
  trendingStocks: State<List<Quote>>,
  onQuoteClick: (Quote) -> Unit
) {
  items(
      count = trendingStocks.value.size,
      key = { i -> trendingStocks.value[i].symbol }
  ) { i ->
    val quote = trendingStocks.value[i]
    QuoteCard(quote = quote, onClick = onQuoteClick, quoteNameMaxLines = 1)
  }
}
