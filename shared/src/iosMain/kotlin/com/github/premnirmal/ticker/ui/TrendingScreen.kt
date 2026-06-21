package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsCard
import com.github.premnirmal.ticker.news.NewsFeedScreen
import com.github.premnirmal.ticker.news.NewsFeedViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSURL
import platform.UIKit.UIApplication

private object TrendingKoin : KoinComponent {
    val newsProvider: NewsProvider by inject()
}

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/**
 * iOS Trending tab. Drives the shared [NewsFeedViewModel] (built from the iOS Koin graph's
 * [NewsProvider]) through the shared [NewsFeedScreen]. The Android-coupled card slots are supplied
 * here with iOS-native Material 3 cards: a lightweight quote card and the shared Coil-backed
 * [NewsCard] (its `card` container slot a Material 3 [Card]); tapping a news article opens its URL in
 * the system browser via [UIApplication]. Tapping a trending quote navigates to the shared
 * quote-detail destination via [onQuoteClick].
 */
@Composable
fun TrendingScreen(
    onQuoteClick: (Quote) -> Unit = {}
) {
    val viewModel = remember { NewsFeedViewModel(TrendingKoin.newsProvider) }
    NewsFeedScreen(
        viewModel = viewModel,
        newsFeedTitle = "Trending",
        errorText = "Could not fetch news",
        onQuoteClick = onQuoteClick,
        quoteCard = { quote, cardModifier, onClick ->
            TrendingQuoteCard(quote = quote, modifier = cardModifier, onClick = onClick)
        },
        newsCard = { article ->
            NewsCard(
                item = article,
                placeholderColor = MaterialTheme.colorScheme.surfaceVariant,
                onClick = { openArticle(article) },
                card = { onClick, content -> ArticleCard(onClick = onClick, content = content) }
            )
        }
    )
}

@Composable
private fun TrendingQuoteCard(
    quote: Quote,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Card(modifier = modifier, onClick = onClick) {
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
private fun ArticleCard(
    onClick: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth(), onClick = onClick) {
        content()
    }
}

private fun openArticle(article: NewsArticle) {
    val url = article.url ?: return
    val nsUrl = NSURL.URLWithString(url) ?: return
    UIApplication.sharedApplication.openURL(nsUrl)
}
