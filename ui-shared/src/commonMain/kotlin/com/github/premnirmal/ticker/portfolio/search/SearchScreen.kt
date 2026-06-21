package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.NewsFeedItem
import com.github.premnirmal.ticker.news.NewsFeedViewModel
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.ContentType.SINGLE_PANE
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.ticker.ui.fadingEdges
import org.koin.compose.viewmodel.koinViewModel

/**
 * The search + trending screen.
 *
 * Android coupling is hoisted behind the established seam pattern so the layout/behaviour is shared
 * and reusable from iOS: the title, the search field label, the error/empty copy and the
 * [QuoteCard]/[SuggestionItem] labels are plain [String]/[Painter] parameters; the quote and
 * news-article taps are hoisted to [onQuoteClick]/[onArticleClick]; the adaptive two-pane layout is
 * hoisted to a [twoPane] slot (Android supplies an Accompanist `TwoPane` over the window
 * `DisplayFeature`s at the `:app` call site, with [contentType] computed there); and the add/remove
 * dialog is hoisted to the [addSymbolDialog] slot. The shared [SearchViewModel]/[NewsFeedViewModel]
 * are resolved from the Koin graph via the multiplatform [koinViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    contentType: ContentType,
    titleText: String,
    searchLabel: String,
    noDataText: String,
    suggestionsErrorText: String,
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
    clearIcon: Painter,
    suggestionAddIcon: Painter,
    onQuoteClick: (Quote) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    onSuggestionsError: () -> Unit,
    twoPane: @Composable (first: @Composable () -> Unit, second: @Composable () -> Unit) -> Unit,
    addSymbolDialog: @Composable (symbol: String, onDismissRequest: () -> Unit) -> Unit,
    modifier: Modifier = Modifier,
    selectedWidgetId: Int? = null,
    searchViewModel: SearchViewModel = koinViewModel(),
    newsViewModel: NewsFeedViewModel = koinViewModel(),
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val searchResults by searchViewModel.searchResult.collectAsStateWithLifecycle()
    val trendingStocks by searchViewModel.trendingStocks.collectAsStateWithLifecycle(emptyList())
    val isRefreshing by searchViewModel.isRefreshing.collectAsStateWithLifecycle()
    LaunchedEffect(searchViewModel) {
        searchViewModel.suggestionsError.collect {
            onSuggestionsError()
        }
    }
    LaunchedEffect(trendingStocks.isEmpty()) {
        if (trendingStocks.isEmpty()) {
            searchViewModel.fetchTrending()
        }
    }
    val onSuggestionClick: (Suggestion) -> Unit = {
        onQuoteClick(Quote(it.symbol))
    }
    var showAddRemoveForSuggestion by remember { mutableStateOf<Suggestion?>(null) }
    val onSuggestionAddRemoveClick: (Suggestion) -> Unit = { suggestion ->
        showAddRemoveForSuggestion = suggestion
    }
    Scaffold(
        contentWindowInsets = WindowInsets(0.dp),
        modifier = modifier.background(MaterialTheme.colorScheme.surface),
        topBar = {
            Column {
                TopBar(text = titleText)
                SearchInputField(
                    searchQuery = searchQuery,
                    label = searchLabel,
                    clearIcon = clearIcon,
                ) {
                    searchQuery = it
                    searchViewModel.fetchResults(searchQuery)
                }
            }
        }
    ) { padding ->
        val searchAndTrending: @Composable (StaggeredGridCells) -> Unit = { columns ->
            SearchAndTrending(
                columns = columns,
                trendingStocks = trendingStocks,
                onQuoteClick = onQuoteClick,
                searchResults = searchResults,
                onSuggestionClick = onSuggestionClick,
                onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                selectedWidgetId = selectedWidgetId,
                suggestionAddIcon = suggestionAddIcon,
                suggestionsErrorText = suggestionsErrorText,
                holdingsLabel = holdingsLabel,
                dayChangeLabel = dayChangeLabel,
                changePercentLabel = changePercentLabel,
                gainLabel = gainLabel,
                lossLabel = lossLabel,
                changeAmountLabel = changeAmountLabel,
            )
        }
        if (contentType == SINGLE_PANE) {
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isRefreshing = isRefreshing,
                onRefresh = {
                    searchViewModel.fetchTrending()
                },
            ) {
                searchAndTrending(StaggeredGridCells.Adaptive(minSize = 120.dp))
            }
        } else {
            PullToRefreshBox(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                isRefreshing = isRefreshing,
                onRefresh = {
                    searchViewModel.fetchTrending()
                },
            ) {
                twoPane(
                    {
                        searchAndTrending(StaggeredGridCells.Adaptive(minSize = 150.dp))
                    },
                    {
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
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 8.dp),
                                    text = noDataText
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
                                        val article = news[i].article
                                        NewsCard(article, onClick = { onArticleClick(article) })
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
        addSymbolDialog(suggestion.symbol) {
            showAddRemoveForSuggestion = null
        }
    }
}

@Composable
private fun SearchAndTrending(
    columns: StaggeredGridCells,
    trendingStocks: List<Quote>,
    onQuoteClick: (Quote) -> Unit,
    searchResults: FetchResult<List<Suggestion>>?,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Unit,
    selectedWidgetId: Int?,
    suggestionAddIcon: Painter,
    suggestionsErrorText: String,
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
) {
    if (searchResults != null && searchResults.wasSuccessful && !searchResults.dataSafe.isNullOrEmpty()) {
        SearchResults(
            searchResults = searchResults,
            onSuggestionClick = onSuggestionClick,
            onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
            suggestionAddIcon = suggestionAddIcon,
            suggestionsErrorText = suggestionsErrorText,
        )
    } else {
        TrendingStocks(
            columns = columns,
            trendingStocks = trendingStocks,
            onQuoteClick = onQuoteClick,
            selectedWidgetId = selectedWidgetId,
            holdingsLabel = holdingsLabel,
            dayChangeLabel = dayChangeLabel,
            changePercentLabel = changePercentLabel,
            gainLabel = gainLabel,
            lossLabel = lossLabel,
            changeAmountLabel = changeAmountLabel,
        )
    }
}

@Composable
private fun SearchResults(
    searchResults: FetchResult<List<Suggestion>>,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Unit,
    suggestionAddIcon: Painter,
    suggestionsErrorText: String,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.surface),
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
                    addRemoveIcon = suggestionAddIcon,
                    suggestion = suggestion,
                    onSuggestionClick = onSuggestionClick,
                    onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                )
            }
        } else {
            item {
                ErrorState(
                    text = suggestionsErrorText,
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
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
) {
    val gridState = rememberLazyStaggeredGridState()
    if (selectedWidgetId == null) {
        rememberScrollToTopAction(HomeRoute.Search) {
            gridState.animateScrollToItem(0)
        }
    }
    LazyVerticalStaggeredGrid(
        modifier = Modifier
            .fillMaxHeight()
            .padding(
                horizontal = 8.dp
            )
            .background(color = MaterialTheme.colorScheme.surface)
            .fadingEdges(state = gridState),
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
                holdingsLabel = holdingsLabel,
                dayChangeLabel = dayChangeLabel,
                changePercentLabel = changePercentLabel,
                gainLabel = gainLabel,
                lossLabel = lossLabel,
                changeAmountLabel = changeAmountLabel,
                onClick = onQuoteClick,
                quoteNameMaxLines = 1,
            )
        }
    }
}
