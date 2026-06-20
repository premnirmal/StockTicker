package com.github.premnirmal.ticker.news

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.detail.QuoteCard
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.rememberScrollToTopAction
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsFeedItem.ArticleNewsFeed
import com.github.premnirmal.ticker.news.NewsFeedItem.TrendingStockNewsFeed
import com.github.premnirmal.ticker.ui.ErrorState
import com.github.premnirmal.ticker.ui.ProgressState
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.ticker.ui.fadingEdges
import org.koin.compose.viewmodel.koinViewModel

/**
 * The trending news feed screen.
 *
 * Android-resource coupling is hoisted behind the established seam pattern: the title, the
 * error-state copy and the [QuoteCard] position-row labels are plain [String] parameters, and the
 * news-article tap is hoisted to [onArticleClick] (Android opens a Chrome Custom Tab at the `:app`
 * call site), so the `R.string` lookups and the Custom-Tab integration stay in `:app` while the
 * layout/behaviour is shared and reusable from iOS. The shared [NewsFeedViewModel] is resolved from
 * the Koin graph via the multiplatform [koinViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewsFeedScreen(
    title: String,
    errorText: String,
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
    onQuoteClick: (Quote) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NewsFeedViewModel = koinViewModel()
) {
    val newsList by viewModel.newsFeedFlow.collectAsStateWithLifecycle()
    val newsFeed by viewModel.newsFeed.collectAsStateWithLifecycle()
    val isRefreshing by viewModel.isRefreshing.collectAsStateWithLifecycle()
    LaunchedEffect(newsList.isEmpty()) {
        if (newsList.isEmpty()) {
            viewModel.fetchNews()
        }
    }
    Column {
        TopBar(text = title)
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
                        modifier = modifier,
                        newsFeedItems = newsList,
                        holdingsLabel = holdingsLabel,
                        dayChangeLabel = dayChangeLabel,
                        changePercentLabel = changePercentLabel,
                        gainLabel = gainLabel,
                        lossLabel = lossLabel,
                        changeAmountLabel = changeAmountLabel,
                        onQuoteClick = onQuoteClick,
                        onArticleClick = onArticleClick,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NewsFeedItems(
    newsFeedItems: List<NewsFeedItem>,
    holdingsLabel: String,
    dayChangeLabel: String,
    changePercentLabel: String,
    gainLabel: String,
    lossLabel: String,
    changeAmountLabel: String,
    onQuoteClick: (Quote) -> Unit,
    onArticleClick: (NewsArticle) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state = rememberLazyListState()
    rememberScrollToTopAction(HomeRoute.Trending) {
        state.animateScrollToItem(0)
    }
    LazyColumn(
        modifier = modifier.fillMaxWidth().fadingEdges(state = state),
        contentPadding = PaddingValues(all = 8.dp),
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
                                QuoteCard(
                                    quote = quote,
                                    holdingsLabel = holdingsLabel,
                                    dayChangeLabel = dayChangeLabel,
                                    changePercentLabel = changePercentLabel,
                                    gainLabel = gainLabel,
                                    lossLabel = lossLabel,
                                    changeAmountLabel = changeAmountLabel,
                                    modifier = Modifier
                                        .weight(1f, true)
                                        .fillMaxHeight(),
                                    quoteNameMaxLines = 1,
                                    onClick = { onQuoteClick(quote) }
                                )
                            }
                        }
                    }
                }
            } else if (data is ArticleNewsFeed) {
                NewsCard(data.article, onClick = { onArticleClick(data.article) })
            }
        }
    }
}
