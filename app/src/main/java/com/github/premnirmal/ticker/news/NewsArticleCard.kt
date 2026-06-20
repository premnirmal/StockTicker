package com.github.premnirmal.ticker.news

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.network.data.NewsArticle

/**
 * Android host for the shared [NewsCard]: keeps the Chrome Custom-Tab integration (an Android-only
 * concern) in `:app` while the card layout itself is shared via Compose Multiplatform.
 */
@Composable
fun NewsArticleCard(item: NewsArticle) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    NewsCard(item) {
        CustomTabs.openTab(context, item.url, primaryColor.toArgb())
    }
}
