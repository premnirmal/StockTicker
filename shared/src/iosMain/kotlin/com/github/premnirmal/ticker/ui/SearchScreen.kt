package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_close
import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.SuggestionsProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.Suggestion
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object SearchKoin : KoinComponent {
    val suggestionsProvider: SuggestionsProvider by inject()
    val newsProvider: NewsProvider by inject()
    val stocksProvider: IStocksProvider by inject()
}

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/**
 * Drives the shared [SearchScreen] on iOS: it debounces symbol queries through the shared
 * [SuggestionsProvider], loads the trending stocks through the shared [NewsProvider] and toggles a
 * symbol's membership of the watchlist through the shared [IStocksProvider]. Unlike Android — where
 * symbols are added to a specific Glance widget — iOS has a single watchlist, so the add/remove
 * action operates on the shared portfolio directly.
 */
class IosSearchViewModel(
    private val suggestionsProvider: SuggestionsProvider,
    private val newsProvider: NewsProvider,
    private val stocksProvider: IStocksProvider,
) : ViewModel() {

    val searchResult: StateFlow<FetchResult<List<Suggestion>>?>
        get() = _searchResult
    private val _searchResult = MutableStateFlow<FetchResult<List<Suggestion>>?>(null)

    val trendingStocks: StateFlow<List<Quote>>
        get() = _trendingStocks
    private val _trendingStocks = MutableStateFlow<List<Quote>>(emptyList())

    val isRefreshing: StateFlow<Boolean>
        get() = _isRefreshing
    private val _isRefreshing = MutableStateFlow(false)

    val portfolioSymbols: StateFlow<Set<String>>
        get() = _portfolioSymbols
    private val _portfolioSymbols = MutableStateFlow<Set<String>>(emptySet())

    private var searchJob: Job? = null
    private var fetchTrendingJob: Job? = null

    init {
        viewModelScope.launch {
            stocksProvider.portfolio.collect { quotes ->
                _portfolioSymbols.value = quotes.map { it.symbol }.toSet()
            }
        }
    }

    fun fetchTrending() {
        fetchTrendingJob?.cancel()
        fetchTrendingJob = viewModelScope.launch {
            _isRefreshing.value = true
            val trendingResult = newsProvider.fetchTrendingStocks(true)
            if (trendingResult.wasSuccessful) {
                _trendingStocks.value = trendingResult.data
            }
            _isRefreshing.value = false
        }
    }

    fun fetchResults(query: String) {
        if (query.isEmpty()) {
            _searchResult.value = FetchResult.success(emptyList())
            return
        }
        searchJob?.cancel()
        searchJob = viewModelScope.launch {
            delay(500L)
            val result = suggestionsProvider.fetchSuggestions(query)
            _searchResult.value = result
        }
    }

    fun toggleSymbol(symbol: String) {
        viewModelScope.launch {
            if (stocksProvider.hasTicker(symbol)) {
                stocksProvider.removeStock(symbol)
            } else {
                stocksProvider.addStock(symbol)
            }
        }
    }
}

/**
 * iOS Search tab. Renders the shared [SearchScreen] with iOS-native Material 3 slots: a lightweight
 * trending [quoteCard], a [suggestionItem] whose trailing button adds/removes the symbol from the
 * watchlist, and the shared `ic_close` clear icon. Tapping a trending quote or a suggestion navigates
 * to the shared quote-detail destination via [onQuoteClick].
 */
@Composable
fun SearchScreen(
    onQuoteClick: (Quote) -> Unit = {}
) {
    val viewModel = remember {
        IosSearchViewModel(
            suggestionsProvider = SearchKoin.suggestionsProvider,
            newsProvider = SearchKoin.newsProvider,
            stocksProvider = SearchKoin.stocksProvider,
        )
    }
    val searchResults by viewModel.searchResult.collectAsState()
    val trendingStocks by viewModel.trendingStocks.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val portfolioSymbols by viewModel.portfolioSymbols.collectAsState()

    LaunchedEffect(trendingStocks.isEmpty()) {
        if (trendingStocks.isEmpty()) viewModel.fetchTrending()
    }

    com.github.premnirmal.ticker.portfolio.search.SearchScreen(
        searchTitle = "Search",
        searchFieldLabel = "Enter a symbol",
        suggestionsErrorText = "Could not fetch suggestions",
        clearIcon = painterResource(Res.drawable.ic_close),
        searchResults = searchResults,
        trendingStocks = trendingStocks,
        isRefreshing = isRefreshing,
        onQueryChange = { query -> viewModel.fetchResults(query) },
        onRefresh = { viewModel.fetchTrending() },
        onQuoteClick = onQuoteClick,
        quoteCard = { quote, onClick ->
            SearchQuoteCard(quote = quote, onClick = { onClick(quote) })
        },
        suggestionItem = { suggestion, onSuggestionClick, _ ->
            SuggestionRow(
                suggestion = suggestion,
                inWatchlist = portfolioSymbols.contains(suggestion.symbol),
                onSuggestionClick = onSuggestionClick,
                onAddRemoveClick = { viewModel.toggleSymbol(suggestion.symbol) },
            )
        },
    )
}

@Composable
private fun SearchQuoteCard(
    quote: Quote,
    onClick: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text(text = quote.symbol, style = MaterialTheme.typography.titleMedium)
            Text(
                text = quote.name,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = quote.priceString(), style = MaterialTheme.typography.bodyMedium)
                Text(
                    text = quote.changePercentStringWithSign(),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (quote.isDown) NegativeColor else PositiveColor
                )
            }
        }
    }
}

@Composable
private fun SuggestionRow(
    suggestion: Suggestion,
    inWatchlist: Boolean,
    onSuggestionClick: (Suggestion) -> Unit,
    onAddRemoveClick: () -> Unit,
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().padding(start = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                modifier = Modifier
                    .weight(1f)
                    .clickable { onSuggestionClick(suggestion) },
                text = suggestion.displayString(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
            )
            IconButton(onClick = onAddRemoveClick) {
                Text(
                    text = if (inWatchlist) "\u2212" else "+",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
        HorizontalDivider(modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp))
    }
}
