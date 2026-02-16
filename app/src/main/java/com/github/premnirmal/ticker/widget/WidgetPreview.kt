package com.github.premnirmal.ticker.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.AndroidUiModes.UI_MODE_NIGHT_YES
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import kotlin.random.Random

@Composable
fun GlanceWidgetPreview(
    modifier: Modifier,
    widgetData: SerializableWidgetState,
    quotes: List<Quote>,
) {
    val columns = when {
        widgetData.singleStockPerRow -> 1
        widgetData.layoutType == SerializableLayoutType.MyPortfolio -> 1
        else -> 2
    }
    Box(
        modifier = modifier
            .fillMaxWidth()
            .toBackgroundPainter(widgetData.backgroundResource)
            // .paint(painterResource(widgetData.backgroundResource))
            .padding(6.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth()
        ) {
            if (quotes.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = stringResource(R.string.no_symbols),
                        style = TextStyle(
                            color = colorResource(R.color.text_widget_negative),
                            fontSize = TextUnit(16f, TextUnitType.Sp),
                            textAlign = TextAlign.Center,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                }
            } else {
                if (!widgetData.hideWidgetHeader) {
                    Header(widgetData)
                }
                QuotesGrid(columns, widgetData, quotes)
            }
        }
    }
}

@Composable
private fun Header(
    widgetData: SerializableWidgetState,
) {
    val lastUpdatedText = when (widgetData.fetchState) {
        is SerializableFetchState.Success -> stringResource(R.string.last_fetch, widgetData.fetchState.displayString)
        is SerializableFetchState.Failure -> stringResource(R.string.refresh_failed)
        else -> SerializableFetchState.NotFetched.displayString
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fontSize = widgetData.fontSize - 2f
        Text(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 2.dp),
            text = lastUpdatedText,
            style = TextStyle(
                color = colorResource(R.color.text_widget_header),
                fontSize = TextUnit(fontSize, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun QuotesGrid(
    columns: Int,
    widgetData: SerializableWidgetState,
    quotes: List<Quote>,
) {
    val changeType by remember(widgetData) { mutableStateOf(widgetData.changeType) }
    val textColor = colorResource(widgetData.textColor)
    val fontSize = widgetData.fontSize
    val layoutType = remember(widgetData) { widgetData.layoutType }
    val isBold = widgetData.boldText
    LazyVerticalGrid(
        modifier = Modifier,
        columns = GridCells.Fixed(columns),
    ) {
        items(quotes.size) {
            val stock = quotes[it]
            val changeValueFormatted = stock.changeString()
            val changePercentFormatted = stock.changePercentString()
            val priceFormatted = remember(widgetData.showCurrency) {
                if (widgetData.showCurrency) {
                    stock.priceFormat.format(stock.lastTradePrice)
                } else {
                    stock.priceString()
                }
            }
            val displayName = stock.properties?.displayname.takeUnless { it.isNullOrBlank() } ?: stock.symbol
            val change = stock.change
            val changeInPercent = stock.changeInPercent
            val changeColor = colorResource(widgetData.getChangeColor(change, changeInPercent))
            val changeFormatted = remember(changeType) {
                when (changeType) {
                    SerializableChangeType.Value -> changeValueFormatted
                    SerializableChangeType.Percent -> changePercentFormatted
                }
            }

            if (layoutType == SerializableLayoutType.MyPortfolio) {
                // MyPortfolio(
                //     stock = stock,
                //     widgetData = widgetData,
                // )
            } else {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 2.dp),
                        text = displayName,
                        style = TextStyle(
                            color = textColor,
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        modifier = Modifier.padding(horizontal = 2.dp),
                        text = priceFormatted,
                        style = TextStyle(
                            color = textColor,
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )

                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 2.dp),
                        text = changeFormatted,
                        style = TextStyle(
                            color = changeColor,
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )

                    if (layoutType == SerializableLayoutType.Tabs) {
                        Text(
                            modifier = Modifier
                                .weight(1f)
                                .padding(end = 2.dp),
                            text = changePercentFormatted,
                            style = TextStyle(
                                color = changeColor,
                                fontSize = TextUnit(fontSize, TextUnitType.Sp),
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.End,
                            ),
                            maxLines = 1,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun Modifier.toBackgroundPainter(@DrawableRes resource: Int): Modifier {
    return when (resource) {
        R.drawable.transparent_widget_bg -> { this }
        R.drawable.translucent_widget_bg -> {
            this.background(
                color = colorResource(R.color.translucent),
                shape = RoundedCornerShape(12.dp),
            )
        }
        else -> {
            // R.drawable.app_widget_background
            // R.drawable.app_widget_background_dark
            this.background(
                color = colorResource(R.color.widget_bg),
                shape = RoundedCornerShape(12.dp),
            )
        }
    }
}

@Composable
@Preview(uiMode = UI_MODE_NIGHT_YES)
private fun PreviewDark() {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState()
        GlanceWidgetPreview(
            modifier = Modifier.width(300.dp),
            quotes = listOf(
                fakeQuote("AAPL", fakePosition("AAPL")),
                fakeQuote("MSFT", fakePosition("MSFT")),
                fakeQuote("GOOG", fakePosition("GOOG")),
                fakeQuote("AMZN", fakePosition("AMZN")),
                fakeQuote("BRK-B", fakePosition("BRK-B"))
            ),
            widgetData = data,
        )
    }
}

@Composable
@Preview
private fun Preview() {
    Box(modifier = Modifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState()
        GlanceWidgetPreview(
            modifier = Modifier.width(300.dp),
            quotes = listOf(
                fakeQuote("AAPL", fakePosition("AAPL")),
                fakeQuote("MSFT", fakePosition("MSFT")),
                fakeQuote("GOOG", fakePosition("GOOG")),
                fakeQuote("AMZN", fakePosition("AMZN")),
                fakeQuote("BRK-B", fakePosition("BRK-B"))
            ),
            widgetData = data,
        )
    }
}

private fun fakeQuote(
    symbol: String,
    position: Position? = null
): Quote {
    return Quote(
        symbol = symbol,
        position = position,
        lastTradePrice = Random.nextDouble(122434.4242).toFloat(),
        change = Random.nextDouble(48.0).toFloat(),
        changeInPercent = Random.nextDouble(12.0).toFloat(),
    )
}

private fun fakePosition(symbol: String): Position {
    return Position(
        symbol = symbol,
        holdings = mutableListOf(
            Holding(
                symbol = symbol,
                shares = Random.nextDouble(10.0).toFloat(),
                price = Random.nextDouble(1434.4242).toFloat(),
            )
        )
    )
}

private fun previewDataState(
    layoutType: IWidgetData.LayoutType = IWidgetData.LayoutType.Fixed,
): SerializableWidgetState = SerializableWidgetState(
    layoutType = SerializableLayoutType.from(layoutType),
    showCurrency = false,
    boldText = false,
    changeType = SerializableChangeType.Percent,
    sizePref = 0,
    fontSize = 12f,
    isDarkMode = false,
    hideWidgetHeader = false,
    negativeTextColor = R.color.text_widget_negative,
    positiveTextColor = R.color.text_widget_positive,
    textColor = R.color.widget_text,
    backgroundResource = R.drawable.app_widget_background,
    isRefreshing = false,
    fetchState = SerializableFetchState.Success(System.currentTimeMillis()),
)
