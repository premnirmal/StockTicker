package com.github.premnirmal.ticker.news

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.tickerwidget.ui.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme

/**
 * Thin Android host for the shared [NewsCard][com.github.premnirmal.ticker.news.NewsCard].
 *
 * Supplies the Android-coupled inputs the shared card hoists: the Custom Tabs article tap, the
 * `ColourPalette` image placeholder colour and the `AppCard` container (it lives in `:UI`).
 */
@Composable
fun NewsCard(item: NewsArticle) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    NewsCard(
        item = item,
        placeholderColor = ColourPalette.ImagePlaceHolderGray,
        onClick = {
            CustomTabs.openTab(context, item.url, primaryColor.toArgb())
        },
        card = { onClick, content ->
            AppCard(onClick = onClick, content = content)
        }
    )
}

@Preview
@Composable
fun NewsCardPreview() {
    AppTheme(theme = SelectedTheme.LIGHT) {
        Column {
            NewsCard(
                NewsArticle(
                    title = "Lorem ipsum testing this is a long news article",
                    url = "https://news.google.com/xyz",
                    publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT"
                )
            )
            NewsCard(
                NewsArticle(
                    title = "Lorem ipsum testing this is a long news article lorem ipsum " +
                        "testing this is a long news article lorem ipsum testing",
                    url = "https://news.google.com/xyz",
                    publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT",
                    imageUrl = "https://example.com/image.jpg"
                )
            )
            NewsCard(
                NewsArticle(
                    title = "Lorem ipsum testing this is a long news article",
                    url = "https://news.google.com/xyz",
                    publishedAt = "Tue, 3 Jun 2008 11:05:30 GMT",
                    imageUrl = "https://example.com/image.jpg"
                )
            )
        }
    }
}
