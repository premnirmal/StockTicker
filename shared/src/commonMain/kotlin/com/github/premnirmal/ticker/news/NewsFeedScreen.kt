package com.github.premnirmal.ticker.news

import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.navigation.LocalContentBottomPadding
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.NewsFeedItem.TrendingStockNewsFeed
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.ProgressState
import com.github.premnirmal.ticker.ui.TopBar

/**
 * Trending/news feed screen, shared by Android and iOS. The platform-specific inputs are hoisted as
 * parameters so the screen has no Android dependencies:
 *  - the localised labels ([newsFeedTitle]/[errorText]) as [String]s,
 *  - the article and quote cards as composable slots ([newsCard]/[quoteCard]) — they still pull in
 *    the (not-yet-shared) image loading + theme on Android,
 *  - the list fading-edge decoration as [listFadingEdges] (Android `RuntimeShader`),
 *  - the navigation scroll-to-top registration as [registerScrollToTop],
 *  - the quote tap as [onQuoteClick].
 * The Android `NewsFeedScreen` host in `:app` supplies them.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    viewModel: NewsFeedViewModel,
    newsFeedTitle: String,
    errorText: String,
    onQuoteClick: (Quote) -> Unit,
    quoteCard: @Composable (quote: Quote, modifier: Modifier, onClick: () -> Unit) -> Unit,
    newsCard: @Composable (article: NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit = {},
) {
    val newsList by viewModel.newsFeedFlow.collectAsState()
    val newsFeed by viewModel.newsFeed.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    LaunchedEffect(newsList.isEmpty()) {
        if (newsList.isEmpty()) {
            viewModel.fetchNews()
        }
    }
    Column {
        TopBar(text = newsFeedTitle)
        PullToRefreshBox(
            modifier = modifier.fillMaxSize(),
            isRefreshing = isRefreshing,
            onRefresh = {
                viewModel.fetchNews(true)
            },
        ) {
            when (newsFeed?.wasSuccessful) {
                true -> {
                    NewsFeedItems(
                        newsFeedItems = newsList,
                        onQuoteClick = onQuoteClick,
                        quoteCard = quoteCard,
                        newsCard = newsCard,
                        modifier = modifier,
                        listFadingEdges = listFadingEdges,
                        registerScrollToTop = registerScrollToTop,
                    )
                }

                false -> {
                    ErrorState(modifier = modifier, text = errorText)
                }

                else -> {
                    ProgressState(modifier = modifier)
                }
            }
        }
    }
}

@Composable
private fun NewsFeedItems(
    newsFeedItems: List<NewsFeedItem>,
    onQuoteClick: (Quote) -> Unit,
    quoteCard: @Composable (quote: Quote, modifier: Modifier, onClick: () -> Unit) -> Unit,
    newsCard: @Composable (article: NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
    listFadingEdges: (ScrollableState) -> Modifier = { Modifier },
    registerScrollToTop: @Composable (scrollToTop: suspend () -> Unit) -> Unit = {},
) {
    val state = rememberLazyListState()
    registerScrollToTop {
        state.animateScrollToItem(0)
    }
    val bottomNavPadding = LocalContentBottomPadding.current
    LazyColumn(
        modifier = modifier.fillMaxWidth().then(listFadingEdges(state)),
        contentPadding = PaddingValues(start = 8.dp, top = 8.dp, end = 8.dp, bottom = 8.dp + bottomNavPadding),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        state = state,
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
                    val trending = data.quotes.withIndex().groupBy { it.index / 3 }.map { it.value.map { it.value } }
                    trending.forEach { group ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(intrinsicSize = IntrinsicSize.Max),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            group.forEach { quote ->
                                quoteCard(
                                    quote,
                                    Modifier
                                        .weight(1f, true)
                                        .fillMaxHeight()
                                ) { onQuoteClick(quote) }
                            }
                        }
                    }
                }
            } else if (data is ArticleNewsFeed) {
                newsCard(data.article)
            }
        }
    }
}
