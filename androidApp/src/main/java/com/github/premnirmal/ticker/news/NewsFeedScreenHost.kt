package com.github.premnirmal.ticker.news

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.ui.fadingEdges
import com.github.premnirmal.tickerwidget.R
import org.koin.androidx.compose.koinViewModel

/**
 * Android host for the shared [NewsFeedScreen]. Resolves the Koin [NewsFeedViewModel], the localised
 * labels, the Android `QuoteCard`/`NewsCard` slots, the `RuntimeShader`-based [fadingEdges] and the
 * navigation [rememberScrollToTopAction] registration, then delegates to the shared screen.
 */
@Composable
fun NewsFeedScreen(
    modifier: Modifier = Modifier,
    onQuoteClick: (Quote) -> Unit,
    viewModel: NewsFeedViewModel = koinViewModel()
) {
    NewsFeedScreen(
        viewModel = viewModel,
        newsFeedTitle = stringResource(id = R.string.news_feed),
        errorText = stringResource(id = R.string.error_fetching_news),
        onQuoteClick = onQuoteClick,
        quoteCard = { quote, cardModifier, onClick ->
            QuoteCard(
                quote = quote,
                modifier = cardModifier,
                quoteNameMaxLines = 1,
                onClick = { onClick() }
            )
        },
        newsCard = { article -> NewsCard(article) },
        modifier = modifier,
        listFadingEdges = { state: ScrollableState -> Modifier.fadingEdges(state) },
        registerScrollToTop = { scrollToTop ->
            rememberScrollToTopAction(HomeRoute.Trending, scrollToTop = scrollToTop)
        },
    )
}
