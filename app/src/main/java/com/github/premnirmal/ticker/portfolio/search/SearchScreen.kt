package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
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
    selectedWidgetId: Int? = null,
    searchViewModel: SearchViewModel = hiltViewModel(),
    newsViewModel: NewsFeedViewModel = hiltViewModel(),
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchResults by searchViewModel.searchResult.collectAsStateWithLifecycle()
    val trendingStocks by searchViewModel.trendingStocks.collectAsStateWithLifecycle(emptyList())
    val isRefreshing by searchViewModel.isRefreshing.collectAsStateWithLifecycle()
    LaunchedEffect(trendingStocks.isEmpty()) {
        if (trendingStocks.isEmpty()) {
            searchViewModel.fetchTrending()
        }
    }
    val onSuggestionClick: (Suggestion) -> Unit = {
        onQuoteClick(Quote(it.symbol))
    }
    val hasWidgets by searchViewModel.hasWidget.collectAsStateWithLifecycle(false)
    val widgetData by searchViewModel.widgetData.collectAsStateWithLifecycle(emptyList())
    var showAddRemoveForSuggestion by remember { mutableStateOf<Suggestion?>(null) }
    val onSuggestionAddRemoveClick: (Suggestion) -> Boolean = { suggestion ->
        if (!suggestion.exists && hasWidgets && searchViewModel.widgetCount > 1 && selectedWidgetId == null) {
            showAddRemoveForSuggestion = suggestion
        } else if (selectedWidgetId != null) {
            suggestion.exists = !suggestion.exists
            if (suggestion.exists) {
                searchViewModel.addTickerToWidget(suggestion.symbol, selectedWidgetId)
            } else {
                searchViewModel.removeStock(suggestion.symbol, selectedWidgetId)
            }
        } else {
            widgetData.firstOrNull()?.let { widgetData ->
                suggestion.exists = !suggestion.exists
                if (suggestion.exists) {
                    searchViewModel.addTickerToWidget(suggestion.symbol, widgetData.widgetId)
                } else {
                    searchViewModel.removeStock(suggestion.symbol, widgetData.widgetId)
                }
            }
        }
        suggestion.exists
    }
    val contentType: ContentType = calculateContentAndNavigationType(
        widthSizeClass = widthSizeClass,
        displayFeatures = displayFeatures
    ).second
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        topBar = {
            Column {
                TopBar(text = stringResource(id = R.string.action_search))
                SearchInputField(searchQuery = searchQuery) {
                    searchQuery = it
                    searchViewModel.fetchResults(searchQuery)
                }
            }
        }
    ) { padding ->
        if (contentType == SINGLE_PANE) {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize().padding(padding),
                isRefreshing = isRefreshing,
                onRefresh = {
                    searchViewModel.fetchTrending()
                },
            ) {
                SearchAndTrending(
                    columns = StaggeredGridCells.Adaptive(minSize = 120.dp),
                    trendingStocks = trendingStocks,
                    onQuoteClick = onQuoteClick,
                    searchResults = searchResults,
                    onSuggestionClick = onSuggestionClick,
                    onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                    selectedWidgetId = selectedWidgetId,
                )
            }
        } else {
            PullToRefreshBox(
                modifier = Modifier.fillMaxSize().padding(padding),
                isRefreshing = isRefreshing,
                onRefresh = {
                    searchViewModel.fetchTrending()
                },
            ) {
                TwoPane(
                    modifier = Modifier,
                    strategy = HorizontalTwoPaneStrategy(
                        splitFraction = 1f / 2f,
                    ),
                    displayFeatures = displayFeatures,
                    foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                    first = {
                        SearchAndTrending(
                            columns = StaggeredGridCells.Adaptive(minSize = 150.dp),
                            trendingStocks = trendingStocks,
                            onQuoteClick = onQuoteClick,
                            searchResults = searchResults,
                            onSuggestionClick = onSuggestionClick,
                            onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                            selectedWidgetId = selectedWidgetId,
                        )
                    },
                    second = {
                        val fetchResult by newsViewModel.newsFeed.collectAsStateWithLifecycle()
                        LaunchedEffect(fetchResult?.dataSafe.isNullOrEmpty()) {
                            if (fetchResult?.dataSafe.isNullOrEmpty()) {
                                newsViewModel.fetchNews()
                            }
                        }
                        fetchResult?.let {
                            val data = it.dataSafe
                            if (data.isNullOrEmpty()) {
                                ErrorState(
                                    modifier = Modifier.fillMaxSize().padding(horizontal = 8.dp),
                                    text = stringResource(id = R.string.no_data)
                                )
                            } else {
                                val news = data.filterIsInstance<NewsFeedItem.ArticleNewsFeed>()
                                val state = rememberLazyListState()
                                if (selectedWidgetId == null) {
                                    rememberScrollToTopAction(HomeRoute.Search) {
                                        state.animateScrollToItem(0)
                                    }
                                }
                                LazyColumn(
                                    modifier = Modifier.padding(horizontal = 8.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp),
                                    state = state,
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

@Composable
private fun SearchAndTrending(
    columns: StaggeredGridCells,
    trendingStocks: List<Quote>,
    onQuoteClick: (Quote) -> Unit,
    searchResults: FetchResult<List<Suggestion>>?,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Boolean,
    selectedWidgetId: Int?,
) {
    if (searchResults != null && searchResults.wasSuccessful && !searchResults.dataSafe.isNullOrEmpty()) {
        SearchResults(
            searchResults = searchResults,
            onSuggestionClick = onSuggestionClick,
            onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
        )
    } else {
        TrendingStocks(
            columns = columns,
            trendingStocks = trendingStocks,
            onQuoteClick = onQuoteClick,
            selectedWidgetId = selectedWidgetId,
        )
    }
}

@Composable
private fun SearchInputField(
    searchQuery: String,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier.fillMaxWidth().padding(8.dp).background(MaterialTheme.colorScheme.surface),
    ) {
        val focusManager = LocalFocusManager.current
        var text by remember {
            mutableStateOf(searchQuery)
        }
        TextField(
            modifier = Modifier.fillMaxWidth(),
            value = text,
            onValueChange = {
                text = it
                onQueryChange(it)
            },
            label = {
                Text(stringResource(id = R.string.enter_a_symbol))
            },
            singleLine = true,
            keyboardActions = KeyboardActions {
                focusManager.clearFocus(force = true)
            },
            keyboardOptions = KeyboardOptions.Default.copy(capitalization = KeyboardCapitalization.Characters),
            trailingIcon = {
                IconButton(
                    enabled = text.isNotEmpty(),
                    onClick = {
                        text = ""
                        onQueryChange("")
                        focusManager.clearFocus(force = true)
                    },
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = null
                    )
                }
            }
        )
    }
}

@Composable
private fun SearchResults(
    searchResults: FetchResult<List<Suggestion>>,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Boolean
) {
    LazyColumn(
        modifier = Modifier.fillMaxHeight().background(color = MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(horizontal = 8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = rememberLazyListState(),
    ) {
        if (searchResults.wasSuccessful) {
            val suggestions = searchResults.data
            items(
                count = suggestions.size,
                key = { i -> suggestions[i].symbol + i }
            ) { i ->
                val suggestion = suggestions[i]
                SuggestionItem(
                    suggestion = suggestion,
                    onSuggestionClick = onSuggestionClick,
                    onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                )
            }
        } else {
            item {
                ErrorState(
                    text = stringResource(R.string.error_fetching_suggestions),
                )
            }
        }
    }
}

@Composable
private fun TrendingStocks(
    columns: StaggeredGridCells,
    trendingStocks: List<Quote>,
    onQuoteClick: (Quote) -> Unit,
    selectedWidgetId: Int?,
) {
    val gridState = rememberLazyStaggeredGridState()
    if (selectedWidgetId == null) {
        rememberScrollToTopAction(HomeRoute.Search) {
            gridState.animateScrollToItem(0)
        }
    }
    LazyVerticalStaggeredGrid(
        modifier = Modifier.fillMaxHeight().padding(
            horizontal = 8.dp
        ).background(color = MaterialTheme.colorScheme.surface),
        columns = columns,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        state = gridState,
    ) {
        items(
            count = trendingStocks.size,
            key = { i -> trendingStocks[i].symbol }
        ) { i ->
            val quote = trendingStocks[i]
            QuoteCard(
                quote = quote,
                onClick = onQuoteClick,
                quoteNameMaxLines = 1,
            )
        }
    }
}
