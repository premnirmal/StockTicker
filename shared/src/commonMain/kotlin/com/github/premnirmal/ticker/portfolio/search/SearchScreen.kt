package com.github.premnirmal.ticker.portfolio.search

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.ScrollableState
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.awaitPointerEvent
import androidx.compose.ui.input.pointer.awaitPointerEventScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.navigation.LocalContentBottomPadding
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import com.github.premnirmal.ticker.ui.AppTextFieldDefaultColors
import com.github.premnirmal.ticker.ui.AppTextFieldShape
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.TopBar

/**
 * Search/trending screen, shared by Android and iOS. The screen is stateless: the state it renders
 * and the events it raises are hoisted as parameters so it has no Android, navigation or
 * dependency-injection dependencies:
 *  - the search state ([searchResults]/[trendingStocks]/[isRefreshing]) as plain values,
 *  - the search/refresh/navigation events as lambdas ([onQueryChange]/[onRefresh]/[onQuoteClick]),
 *  - the localised labels ([searchTitle]/[searchFieldLabel]/[suggestionsErrorText]) as [String]s,
 *  - the clear-query icon as a [Painter] ([clearIcon]),
 *  - the quote card, suggestion row and add/remove dialog as composable slots
 *    ([quoteCard]/[suggestionItem]/[addSymbolDialog]) — they still pull in the (not-yet-shared)
 *    theme/resources on Android,
 *  - the list fading-edge decoration as [listFadingEdges] (Android `RuntimeShader`),
 *  - the navigation scroll-to-top registration as [registerScrollToTop],
 *  - the optional adaptive two-pane layout as [twoPane] (null = single column).
 * The Android `SearchScreen` host in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    searchTitle: String,
    searchFieldLabel: String,
    suggestionsErrorText: String,
    clearIcon: Painter,
    searchResults: FetchResult<List<Suggestion>>?,
    trendingStocks: List<Quote>,
    isRefreshing: Boolean,
    onQueryChange: (String) -> Unit,
    onRefresh: () -> Unit,
    onQuoteClick: (Quote) -> Unit,
    quoteCard: @Composable (quote: Quote, onClick: (Quote) -> Unit) -> Unit,
    suggestionItem: @Composable (
        suggestion: Suggestion,
        onSuggestionClick: (Suggestion) -> Unit,
        onSuggestionAddRemoveClick: (Suggestion) -> Unit
    ) -> Unit,
    modifier: Modifier = Modifier,
    selectedWidgetId: Int? = null,
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit = {},
    addSymbolDialog: @Composable (symbol: String, onDismissRequest: () -> Unit) -> Unit = { _, _ -> },
    clearFocusOnContentTap: Boolean = false,
    twoPane: (@Composable (first: @Composable () -> Unit) -> Unit)? = null,
) {
    var searchQuery by rememberSaveable { mutableStateOf("") }
    val focusManager = LocalFocusManager.current
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
                TopBar(text = searchTitle)
                SearchInputField(
                    searchQuery = searchQuery,
                    label = searchFieldLabel,
                    clearIcon = clearIcon,
                ) {
                    searchQuery = it
                    onQueryChange(it)
                }
            }
        }
    ) { padding ->
        val searchAndTrending: @Composable (Dp) -> Unit = { minSize ->
            SearchAndTrending(
                columns = StaggeredGridCells.Adaptive(minSize = minSize),
                trendingStocks = trendingStocks,
                onQuoteClick = onQuoteClick,
                searchResults = searchResults,
                suggestionsErrorText = suggestionsErrorText,
                onSuggestionClick = onSuggestionClick,
                onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
                quoteCard = quoteCard,
                suggestionItem = suggestionItem,
                selectedWidgetId = selectedWidgetId,
                listFadingEdges = listFadingEdges,
                registerScrollToTop = registerScrollToTop,
            )
        }
        PullToRefreshBox(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .pointerInput(clearFocusOnContentTap) {
                    if (!clearFocusOnContentTap) {
                        return@pointerInput
                    }
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial)
                            focusManager.clearFocus(force = true)
                        }
                    }
                },
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
        ) {
            if (twoPane == null) {
                searchAndTrending(120.dp)
            } else {
                twoPane {
                    searchAndTrending(150.dp)
                }
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
    suggestionsErrorText: String,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Unit,
    quoteCard: @Composable (quote: Quote, onClick: (Quote) -> Unit) -> Unit,
    suggestionItem: @Composable (
        suggestion: Suggestion,
        onSuggestionClick: (Suggestion) -> Unit,
        onSuggestionAddRemoveClick: (Suggestion) -> Unit
    ) -> Unit,
    selectedWidgetId: Int?,
    listFadingEdges: (ScrollableState) -> Modifier,
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit,
) {
    if (searchResults != null && searchResults.wasSuccessful && !searchResults.dataSafe.isNullOrEmpty()) {
        SearchResults(
            searchResults = searchResults,
            suggestionsErrorText = suggestionsErrorText,
            onSuggestionClick = onSuggestionClick,
            onSuggestionAddRemoveClick = onSuggestionAddRemoveClick,
            suggestionItem = suggestionItem,
        )
    } else {
        TrendingStocks(
            columns = columns,
            trendingStocks = trendingStocks,
            onQuoteClick = onQuoteClick,
            quoteCard = quoteCard,
            selectedWidgetId = selectedWidgetId,
            listFadingEdges = listFadingEdges,
            registerScrollToTop = registerScrollToTop,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SearchInputField(
    searchQuery: String,
    label: String,
    clearIcon: Painter,
    onQueryChange: (String) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(8.dp),
    ) {
        val focusManager = LocalFocusManager.current
        var text by remember {
            mutableStateOf(searchQuery)
        }
        TextField(
            shape = AppTextFieldShape,
            modifier = Modifier.fillMaxWidth(),
            colors = AppTextFieldDefaultColors,
            value = text,
            onValueChange = {
                text = it
                onQueryChange(it)
            },
            label = {
                Text(label)
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
                        painter = clearIcon,
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
    suggestionsErrorText: String,
    onSuggestionClick: (Suggestion) -> Unit,
    onSuggestionAddRemoveClick: (Suggestion) -> Unit,
    suggestionItem: @Composable (
        suggestion: Suggestion,
        onSuggestionClick: (Suggestion) -> Unit,
        onSuggestionAddRemoveClick: (Suggestion) -> Unit
    ) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxHeight()
            .background(color = MaterialTheme.colorScheme.surface),
        contentPadding = PaddingValues(start = 8.dp, end = 8.dp, bottom = LocalContentBottomPadding.current),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = rememberLazyListState(),
    ) {
        if (searchResults.wasSuccessful) {
            val suggestions = searchResults.data
            items(
                count = suggestions.size,
                key = { i -> suggestions[i].symbol + i }
            ) { i ->
                suggestionItem(
                    suggestions[i],
                    onSuggestionClick,
                    onSuggestionAddRemoveClick,
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
    quoteCard: @Composable (quote: Quote, onClick: (Quote) -> Unit) -> Unit,
    selectedWidgetId: Int?,
    listFadingEdges: (ScrollableState) -> Modifier,
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit,
) {
    val gridState = rememberLazyStaggeredGridState()
    if (selectedWidgetId == null) {
        registerScrollToTop {
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
            .then(listFadingEdges(gridState)),
        columns = columns,
        contentPadding = PaddingValues(bottom = LocalContentBottomPadding.current),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalItemSpacing = 8.dp,
        state = gridState,
    ) {
        items(
            count = trendingStocks.size,
            key = { i -> trendingStocks[i].symbol }
        ) { i ->
            quoteCard(trendingStocks[i], onQuoteClick)
        }
    }
}
