package com.github.premnirmal.ticker.news

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.network.data.NewsArticle
import com.github.premnirmal.tickerwidget.ui.theme.AppCard
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme

@Composable
fun NewsCard(item: NewsArticle) {
    val context = LocalContext.current
    val primaryColor = MaterialTheme.colorScheme.primary
    AppCard(onClick = {
        CustomTabs.openTab(context, item.url, primaryColor.toArgb())
    }) {
        Column(modifier = Modifier.padding(all = 8.dp)) {
            Row {
                Text(
                    modifier = Modifier.weight(1f),
                    text = item.sourceName(),
                    maxLines = 1,
                    style = MaterialTheme.typography.labelSmall
                )
                Text(
                    textAlign = TextAlign.End,
                    text = item.dateString(),
                    maxLines = 1,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Row(modifier = Modifier.padding(top = 4.dp)) {
                Text(
                    modifier = Modifier.weight(1f).padding(top = 8.dp),
                    text = item.titleSanitized(),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.bodyMedium
                )
                item.imageUrl?.let {
                    var sizeImage by remember { mutableStateOf(IntSize.Zero) }
                    val gradient = Brush.verticalGradient(
                        colors = listOf(ColourPalette.ImagePlaceHolderGray, MaterialTheme.colorScheme.surfaceVariant),
                        startY = sizeImage.height.toFloat(),
                        endY = 0f
                    )
                    Box(modifier = Modifier.padding(start = 8.dp)) {
                        Box(
                            modifier = Modifier
                                .width(70.dp)
                                .height(70.dp)
                                .background(gradient)
                        )
                        AsyncImage(
                            model = it,
                            contentDescription = null,
                            modifier = Modifier
                                .width(70.dp)
                                .height(70.dp)
                                .onGloballyPositioned {
                                    sizeImage = it.size
                                },
                            contentScale = ContentScale.Crop
                        )
                    }
                }
            }
        }
    }
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
                    title = "Lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article lorem ipsum testing this is a long news article",
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
