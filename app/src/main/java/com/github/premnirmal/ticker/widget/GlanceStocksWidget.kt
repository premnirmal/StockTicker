package com.github.premnirmal.ticker.widget

import android.content.Context
import android.widget.RemoteViews
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.Image
import androidx.glance.ImageProvider
import androidx.glance.LocalContext
import androidx.glance.LocalSize
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.AndroidRemoteViews
import androidx.glance.appwidget.CircularProgressIndicator
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.state.updateAppWidgetState
import androidx.glance.background
import androidx.glance.currentState
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.layout.size
import androidx.glance.layout.wrapContentHeight
import androidx.glance.layout.wrapContentSize
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
import androidx.glance.state.GlanceStateDefinition
import androidx.glance.text.FontStyle
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.home.HomeActivity
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject
import kotlin.random.Random

class GlanceStocksWidget : GlanceAppWidget() {

    @Inject
    internal lateinit var stocksProvider: StocksProvider

    @Inject
    internal lateinit var widgetDataProvider: WidgetDataProvider

    @Inject
    internal lateinit var appPreferences: AppPreferences

    var injected = false

    override val sizeMode = SizeMode.Exact

    override val stateDefinition: GlanceStateDefinition<WidgetGlanceState> = WidgetGlanceStateDefinition

    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        if (!injected) {
            Injector.appComponent().inject(this)
            injected = true
        }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)

        // Update the Glance state with current widget data and quotes
        val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
        updateAppWidgetState(
            context = context,
            definition = stateDefinition,
            glanceId = id
        ) { state ->
            val currentQuotes = widgetData.stocks.value
            val currentState = widgetData.data.value
            val currentFetchState = stocksProvider.fetchState.value
            val currentIsRefreshing = appPreferences.isRefreshing.value
            state.copy(
                widgetState = SerializableWidgetState.from(
                    state = currentState,
                    fetchState = currentFetchState,
                    isRefreshing = currentIsRefreshing,
                ),
                quotes = currentQuotes,
            )
        }

        provideContent {
            val glanceState = currentState<WidgetGlanceState>()
            Content(glanceState)
        }
    }

    @Composable
    private fun Content(glanceState: WidgetGlanceState) {
        val colors = WidgetColors.colors()
        GlanceTheme(colors = colors) {
            GlanceWidget(
                widgetData = glanceState.widgetState,
                quotes = glanceState.quotes,
            )
        }
    }
}

@Composable
fun GlanceWidget(
    widgetData: SerializableWidgetState,
    quotes: List<Quote>,
) {
    val context = LocalContext.current
    val size = LocalSize.current
    val width = size.width.value.toInt()
    val columns = when {
        widgetData.singleStockPerRow -> 1
        width > 850 -> 4
        width > 750 -> 3
        width > 250 -> 2
        else -> 1
    }

    Box(
        modifier = GlanceModifier.fillMaxSize()
            .background(ImageProvider(widgetData.backgroundResource))
            .padding(6.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize()
        ) {
            if (quotes.isEmpty()) {
                Box(
                    modifier = GlanceModifier.fillMaxSize().clickable(actionStartActivity<HomeActivity>()),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = context.getString(R.string.no_symbols),
                        style = TextStyle(
                            color = ColorProvider(R.color.text_widget_negative),
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
private fun QuotesGrid(
    columns: Int,
    widgetData: SerializableWidgetState,
    quotes: List<Quote>,
) {
    val context = LocalContext.current
    val changeType by remember(widgetData) { mutableStateOf(widgetData.changeType) }
    val textColor = ColorProvider(widgetData.textColor)
    val fontSize = widgetData.fontSize
    val layoutType = remember(widgetData) { widgetData.layoutType }
    val isBold = widgetData.boldText
    LazyVerticalGrid(
        modifier = GlanceModifier,
        gridCells = GridCells.Fixed(columns),
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
            val changeColor = ColorProvider(widgetData.getChangeColor(change, changeInPercent))
            val changeFormatted = remember(changeType) {
                when (changeType) {
                    SerializableChangeType.Value -> changeValueFormatted
                    SerializableChangeType.Percent -> changePercentFormatted
                }
            }

            if (layoutType == SerializableLayoutType.MyPortfolio) {
                MyPortfolio(
                    stock = stock,
                    widgetData = widgetData,
                )
            } else {
                Row(
                    modifier = GlanceModifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp).clickable(
                            actionStartActivity<HomeActivity>(
                                // actionParametersOf(
                                //     ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                                // )
                            )
                        ),
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
                        modifier = GlanceModifier.padding(horizontal = 2.dp).clickable(
                            actionStartActivity<HomeActivity>(
                                // actionParametersOf(
                                //     ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                                // )
                            )
                        ),
                        text = priceFormatted,
                        style = TextStyle(
                            color = textColor,
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )

                    if (layoutType == SerializableLayoutType.Animated) {
                        val flipper = RemoteViews(context.packageName, R.layout.stockview_flipper)
                        Box(
                            modifier = GlanceModifier.defaultWeight().padding(end = 2.dp).clickable(
                                actionStartActivity<HomeActivity>(
                                    // actionParametersOf(
                                    //     ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                                    // )
                                )
                            ),
                            contentAlignment = Alignment.CenterEnd,
                        ) {
                            AndroidRemoteViews(
                                remoteViews = flipper,
                                containerViewId = R.id.view_flipper,
                                modifier = GlanceModifier.fillMaxWidth()
                            ) {
                                Text(
                                    modifier = GlanceModifier.fillMaxWidth(),
                                    text = changeValueFormatted,
                                    style = TextStyle(
                                        color = changeColor,
                                        fontSize = TextUnit(fontSize, TextUnitType.Sp),
                                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.End,
                                    ),
                                    maxLines = 1,
                                )
                                Text(
                                    modifier = GlanceModifier.fillMaxWidth(),
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
                    } else {
                        Text(
                            modifier = GlanceModifier.defaultWeight().padding(end = 2.dp)
                                .then(
                                    if (layoutType == SerializableLayoutType.Fixed) {
                                        GlanceModifier.clickable(actionRunCallback<FlipTextCallback>())
                                    } else {
                                        GlanceModifier
                                    }
                                ),
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
                                modifier = GlanceModifier.defaultWeight().padding(end = 2.dp),
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
}

@Composable
private fun Header(
    widgetData: SerializableWidgetState,
) {
    val context = LocalContext.current
    val lastUpdatedText = when (widgetData.fetchState) {
        is SerializableFetchState.Success -> context.getString(R.string.last_fetch, widgetData.fetchState.displayString)
        is SerializableFetchState.Failure -> context.getString(R.string.refresh_failed)
        else -> SerializableFetchState.NotFetched.displayString
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fontSize = widgetData.fontSize - 2f
        Text(
            modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
            text = lastUpdatedText,
            style = TextStyle(
                color = ColorProvider(R.color.text_widget_header),
                fontSize = TextUnit(fontSize, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
            ),
        )
    }
}

@Composable
private fun MyPortfolio(
    stock: Quote,
    widgetData: SerializableWidgetState,
) {
    val textColor = ColorProvider(widgetData.textColor)
    val fontSize = widgetData.fontSize
    val gainLossFormatted = stock.gainLossString()
    val gainLossPercentFormatted = stock.gainLossPercentString()
    val priceFormatted = if (widgetData.showCurrency) {
        stock.priceFormat.format(stock.lastTradePrice)
    } else {
        stock.priceString()
    }
    val holdingsFormatted = if (widgetData.showCurrency) {
        stock.priceFormat.format(stock.holdings())
    } else {
        stock.holdingsString()
    }
    val displayName = stock.properties?.displayname.takeUnless { it.isNullOrBlank() } ?: stock.symbol
    val gainLoss = stock.gainLoss()
    val gainLossColor = ColorProvider(widgetData.getChangeColor(gainLoss, gainLoss))
    Column(
        modifier = GlanceModifier.fillMaxSize()
            .clickable(
                actionStartActivity<HomeActivity>(
                    actionParametersOf(
                        ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                    )
                )
            )
    ) {
        Row(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
            Text(
                modifier = GlanceModifier.defaultWeight().padding(end = 2.dp),
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
                modifier = GlanceModifier.padding(end = 2.dp),
                text = holdingsFormatted,
                style = TextStyle(
                    color = textColor,
                    fontSize = TextUnit(fontSize, TextUnitType.Sp),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
            Text(
                modifier = GlanceModifier.defaultWeight().padding(end = 2.dp),
                text = gainLossFormatted,
                style = TextStyle(
                    color = gainLossColor,
                    fontSize = TextUnit(fontSize, TextUnitType.Sp),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
            Text(
                modifier = GlanceModifier.defaultWeight().padding(end = 2.dp),
                text = gainLossPercentFormatted,
                style = TextStyle(
                    color = gainLossColor,
                    fontSize = TextUnit(fontSize, TextUnitType.Sp),
                    textAlign = TextAlign.End,
                    fontWeight = FontWeight.Normal,
                ),
                maxLines = 1,
            )
        }

        Row(modifier = GlanceModifier.fillMaxWidth().wrapContentHeight()) {
            Text(
                modifier = GlanceModifier.defaultWeight(),
                text = priceFormatted,
                style = TextStyle(
                    color = textColor,
                    fontSize = TextUnit(fontSize, TextUnitType.Sp),
                    fontWeight = FontWeight.Normal,
                    textAlign = TextAlign.Start,
                    fontStyle = FontStyle.Italic,
                ),
                maxLines = 1,
            )
        }
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 190, heightDp = 150)
@Composable
private fun WidgetSingleColumnPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.Fixed,
        )
        GlanceWidget(
            widgetData = data,
            quotes = listOf(
                fakeQuote("AAPL"),
                fakeQuote("MSFT"),
                fakeQuote("GOOG"),
                fakeQuote("AMZN"),
                fakeQuote("BRK-B")
            )
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 150)
@Composable
private fun WidgetEmptyPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.Fixed,
        )
        GlanceWidget(
            widgetData = data,
            quotes = emptyList(),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 350, heightDp = 150)
@Composable
private fun WidgetFixedPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.Fixed,
        )
        GlanceWidget(
            widgetData = data,
            quotes = listOf(
                fakeQuote("AAPL"),
                fakeQuote("MSFT"),
                fakeQuote("GOOG"),
                fakeQuote("AMZN"),
                fakeQuote("BRK-B")
            )
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 120)
@Composable
private fun WidgetAnimatedPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.Animated,
        )
        GlanceWidget(
            widgetData = data,
            quotes = listOf(
                fakeQuote("AAPL"),
                fakeQuote("MSFT"),
                fakeQuote("GOOG"),
                fakeQuote("AMZN"),
                fakeQuote("BRK-B")
            )
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 550, heightDp = 140)
@Composable
private fun WidgetTabsPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.Tabs,
        )
        GlanceWidget(
            widgetData = data,
            quotes = listOf(
                fakeQuote("AAPL"),
                fakeQuote("MSFT"),
                fakeQuote("GOOG"),
                fakeQuote("AMZN"),
                fakeQuote("BRK-B")
            )
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 550, heightDp = 200)
@Composable
private fun WidgetMyPortfolioPreview() {
    Box(modifier = GlanceModifier.background(color = MaterialTheme.colorScheme.inverseSurface).padding(20.dp)) {
        val data = previewDataState(
            layoutType = IWidgetData.LayoutType.MyPortfolio,
        )
        GlanceWidget(
            widgetData = data,
            quotes = listOf(
                fakeQuote("AAPL", fakePosition("AAPL")),
                fakeQuote("MSFT", fakePosition("MSFT")),
                fakeQuote("GOOG", fakePosition("GOOG")),
                fakeQuote("AMZN", fakePosition("AMZN")),
                fakeQuote("BRK-B", fakePosition("BRK-B"))
            )
        )
    }
}

private fun fakeQuote(symbol: String, position: Position? = null): Quote {
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

class FlipTextCallback : ActionCallback {
    @Inject
    internal lateinit var widgetDataProvider: WidgetDataProvider
    var injected = false

    override suspend fun onAction(
        context: Context,
        glanceId: GlanceId,
        parameters: ActionParameters
    ) {
        if (!injected) {
            Injector.appComponent().inject(this)
            injected = true
        }
        val glanceAppWidgetManager = GlanceAppWidgetManager(context)
        val appWidgetId = glanceAppWidgetManager.getAppWidgetId(glanceId)
        val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
        // Update Glance state with the flipped change type
        updateAppWidgetState(
            context = context,
            definition = WidgetGlanceStateDefinition,
            glanceId = glanceId,
        ) { currentState ->
            val newChangeType = if (currentState.widgetState.changeType == SerializableChangeType.Value) {
                SerializableChangeType.Percent
            } else {
                SerializableChangeType.Value
            }

            widgetData.setChange(newChangeType == SerializableChangeType.Percent)

            currentState.copy(
                widgetState = currentState.widgetState.copy(changeType = newChangeType)
            )
        }
        widgetDataProvider.broadcastUpdateWidget(appWidgetId)
    }
}
