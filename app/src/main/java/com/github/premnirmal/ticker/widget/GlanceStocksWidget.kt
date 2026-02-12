package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.annotation.DrawableRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.TextUnitType
import androidx.compose.ui.unit.dp
import androidx.glance.ColorFilter
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
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
import androidx.glance.appwidget.GlanceAppWidgetReceiver
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.lazy.GridCells
import androidx.glance.appwidget.lazy.LazyVerticalGrid
import androidx.glance.appwidget.provideContent
import androidx.glance.appwidget.updateAll
import androidx.glance.background
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
import androidx.glance.layout.wrapContentWidth
import androidx.glance.preview.ExperimentalGlancePreviewApi
import androidx.glance.preview.Preview
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
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.Boolean
import kotlin.random.Random

class StockWidget : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = GlanceStocksWidget()

    override fun onReceive(context: Context, intent: Intent) {
        super.onReceive(context, intent)
        if (intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE) {
            CoroutineScope(Dispatchers.Default).launch {
                glanceAppWidget.updateAll(context)
            }
        }
    }
}

class GlanceStocksWidget : GlanceAppWidget() {

    @Inject
    internal lateinit var stocksProvider: StocksProvider

    @Inject
    internal lateinit var widgetDataProvider: WidgetDataProvider

    @Inject
    internal lateinit var appPreferences: AppPreferences

    @Inject
    internal lateinit var coroutineScope: CoroutineScope

    var injected = false

    override val sizeMode = SizeMode.Exact



    override suspend fun provideGlance(
        context: Context,
        id: GlanceId,
    ) {
        if (!injected) {
            Injector.appComponent().inject(this)
            injected = true
        }
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(id)
        val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
        provideContent {
            val quotes by widgetData.stocks.collectAsState()
            val fetchState by stocksProvider.fetchState.collectAsState()
            val refreshing by appPreferences.isRefreshing.collectAsState()
            val data by widgetData.data.collectAsState()
            GlanceWidget(fetchState, data, quotes, refreshing)
        }
    }
}

@Composable
fun GlanceWidget(
    fetchState: FetchState,
    widgetData: WidgetData.ImmutableWidgetData,
    quotes: List<Quote>,
    refreshing: Boolean = false,
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
        modifier = GlanceModifier.fillMaxSize().background(
            ImageProvider(widgetData.backgroundResource())
        ).padding(horizontal = 2.dp)
    ) {
        Column(
            modifier = GlanceModifier.fillMaxSize().padding(6.dp)
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
                    Header(fetchState, widgetData, refreshing)
                }
                QuotesGrid(columns, widgetData, quotes)
            }
        }
    }
}

@Composable
private fun QuotesGrid(
    columns: Int,
    widgetData: WidgetData.ImmutableWidgetData,
    quotes: List<Quote>,
) {
    val context = LocalContext.current
    val textColor = widgetData.widgetTextColor
    val fontSize = widgetData.fontSize
    val changeType = widgetData.changeType
    LazyVerticalGrid(
        modifier = GlanceModifier,
        gridCells = GridCells.Fixed(columns),
    ) {
        val isBold = widgetData.boldText
        val layoutType = widgetData.layoutType

        items(quotes.size) {
            val stock = quotes[it]
            val changeValueFormatted = stock.changeString()
            val changePercentFormatted = stock.changePercentString()
            val priceFormatted = if (widgetData.showCurrency) {
                stock.priceFormat.format(stock.lastTradePrice)
            } else {
                stock.priceString()
            }
            val displayName = stock.properties?.displayname.takeUnless { it.isNullOrBlank() } ?: stock.symbol
            val change = stock.change
            val changeInPercent = stock.changeInPercent
            val changeFormatted = if (changeType == IWidgetData.ChangeType.Percent) {
                changePercentFormatted
            } else {
                changeValueFormatted
            }
            val changeColor = widgetData.getChangeColor(context, change, changeInPercent)

            if (layoutType == IWidgetData.LayoutType.MyPortfolio) {
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
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            textAlign = TextAlign.Start,
                            fontWeight = FontWeight.Medium,
                        ),
                        maxLines = 1,
                    )
                    Text(
                        modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp).clickable(
                            actionStartActivity<HomeActivity>(
                                // actionParametersOf(
                                //     ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                                // )
                            )
                        ),
                        text = priceFormatted,
                        style = TextStyle(
                            color = ColorProvider(textColor),
                            fontSize = TextUnit(fontSize, TextUnitType.Sp),
                            fontWeight = FontWeight.Normal,
                            textAlign = TextAlign.End,
                        ),
                        maxLines = 1,
                    )

                    if (layoutType == IWidgetData.LayoutType.Animated) {
                        val flipper = RemoteViews(context.packageName, R.layout.stockview_flipper)
                        Box(
                            modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp).clickable(
                                actionStartActivity<HomeActivity>(
                                    // actionParametersOf(
                                    //     ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to stock.symbol
                                    // )
                                )
                            ), contentAlignment = Alignment.CenterEnd
                        ) {
                            AndroidRemoteViews(
                                remoteViews = flipper, containerViewId = R.id.view_flipper, modifier = GlanceModifier.wrapContentWidth()
                            ) {
                                Text(
                                    modifier = GlanceModifier,
                                    text = changeValueFormatted,
                                    style = TextStyle(
                                        color = ColorProvider(changeColor),
                                        fontSize = TextUnit(fontSize, TextUnitType.Sp),
                                        fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                        textAlign = TextAlign.End,
                                    ),
                                    maxLines = 1,
                                )
                                Text(
                                    modifier = GlanceModifier,
                                    text = changePercentFormatted,
                                    style = TextStyle(
                                        color = ColorProvider(changeColor),
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
                            modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp)
                                .clickable(actionRunCallback<FlipTextCallback>()),
                            text = changeFormatted,
                            style = TextStyle(
                                color = ColorProvider(changeColor),
                                fontSize = TextUnit(fontSize, TextUnitType.Sp),
                                fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
                                textAlign = TextAlign.End,
                            ),
                            maxLines = 1,
                        )

                        if (layoutType == IWidgetData.LayoutType.Tabs) {
                            Text(
                                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp)
                                    .clickable(actionRunCallback<FlipTextCallback>()),
                                text = changePercentFormatted,
                                style = TextStyle(
                                    color = ColorProvider(changeColor),
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
    fetchState: FetchState,
    widgetData: WidgetData.ImmutableWidgetData,
    refreshing: Boolean
) {
    val context = LocalContext.current
    val lastUpdatedText = when (fetchState) {
        is FetchState.Success -> context.getString(R.string.last_fetch, fetchState.displayString)
        is FetchState.Failure -> context.getString(R.string.refresh_failed)
        else -> FetchState.NotFetched.displayString
    }
    Row(
        modifier = GlanceModifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val fontSize = widgetData.fontSize
        Text(
            modifier = GlanceModifier.defaultWeight().padding(end = 2.dp),
            text = lastUpdatedText,
            style = TextStyle(
                color = ColorProvider(R.color.text_widget_header),
                fontSize = TextUnit(fontSize - 2f, TextUnitType.Sp),
                textAlign = TextAlign.Start,
                fontWeight = FontWeight.Normal,
            ),
        )
        Box(
            modifier = GlanceModifier.wrapContentSize().clickable(actionRunCallback<RefreshCallback>()),
            contentAlignment = Alignment.Center,
        ) {
            if (refreshing) {
                CircularProgressIndicator(
                    modifier = GlanceModifier.size(18.dp),
                    color = ColorProvider(R.color.text_widget_header),
                )
            } else {
                Image(
                    modifier = GlanceModifier.size(18.dp),
                    provider = ImageProvider(R.drawable.ic_refresh),
                    contentDescription = null,
                    colorFilter = ColorFilter.tint(ColorProvider(R.color.text_widget_header)),
                )
            }
        }
    }
}

@Composable
private fun MyPortfolio(
    stock: Quote,
    widgetData: WidgetData.ImmutableWidgetData,
) {
    val context = LocalContext.current
    val textColor = widgetData.widgetTextColor
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
    val gainLossColor = widgetData.getChangeColor(context, gainLoss, gainLoss)
    Column(modifier = GlanceModifier.fillMaxSize()
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
                    color = ColorProvider(textColor),
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
                    color = ColorProvider(textColor),
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
                    color = ColorProvider(gainLossColor),
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
                    color = ColorProvider(gainLossColor),
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
                    color = ColorProvider(textColor),
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
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.Fixed,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
            widgetData = data,
            quotes = listOf(fakeQuote("AAPL"), fakeQuote("MSFT"), fakeQuote("GOOG"), fakeQuote("AMZN"), fakeQuote("BRK-B"))
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 150)
@Composable
private fun WidgetEmptyPreview() {
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.Fixed,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
            widgetData = data,
            quotes = emptyList(),
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 150)
@Composable
private fun WidgetFixedPreview() {
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.Fixed,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
            widgetData = data,
            quotes = listOf(fakeQuote("AAPL"), fakeQuote("MSFT"), fakeQuote("GOOG"), fakeQuote("AMZN"), fakeQuote("BRK-B"))
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 400, heightDp = 120)
@Composable
private fun WidgetAnimatedPreview() {
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.Animated,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
            widgetData = data,
            quotes = listOf(fakeQuote("AAPL"), fakeQuote("MSFT"), fakeQuote("GOOG"), fakeQuote("AMZN"), fakeQuote("BRK-B"))
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 550, heightDp = 140)
@Composable
private fun WidgetTabsPreview() {
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.Tabs,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
            widgetData = data,
            quotes = listOf(fakeQuote("AAPL"), fakeQuote("MSFT"), fakeQuote("GOOG"), fakeQuote("AMZN"), fakeQuote("BRK-B"))
        )
    }
}

@OptIn(ExperimentalGlancePreviewApi::class)
@Preview(widthDp = 550, heightDp = 200)
@Composable
private fun WidgetMyPortfolioPreview() {
    Box(modifier = GlanceModifier.background(color = Color.Black).padding(20.dp)) {
        val widgetData = PreviewWidgetData(
            layoutType = IWidgetData.LayoutType.MyPortfolio,
            isCurrencyEnabled = false,
        )
        val data by widgetData.data.collectAsState()
        GlanceWidget(
            fetchState = fakeFetchState(),
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

private fun fakeFetchState(): FetchState {
    return FetchState.Success(System.currentTimeMillis())
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

private class PreviewWidgetData(
    override val widgetTextColor: Color = Color.Black,
    override val layoutType: IWidgetData.LayoutType = IWidgetData.LayoutType.Fixed,
    override val isCurrencyEnabled: Boolean = false,
    override val isBoldEnabled: Boolean = false,
    override val changeType: IWidgetData.ChangeType = IWidgetData.ChangeType.Percent,
    override val data: StateFlow<WidgetData.ImmutableWidgetData> = MutableStateFlow(
        WidgetData.ImmutableWidgetData(
            id = AppWidgetManager.INVALID_APPWIDGET_ID,
            name = "Preview Widget",
            layoutType = IWidgetData.LayoutType.Fixed,
            showCurrency = false,
            boldText = false,
            changeType = IWidgetData.ChangeType.Percent,
            typePref = IWidgetData.LayoutType.Fixed.ordinal,
            hideWidgetHeader = false,
            autoSort = false,
            sizePref = 0,
            backgroundPref = 0,
            textColourPref = 0,
            backgroundResource = R.drawable.app_widget_background,
            textColor = Color.Black.value,
            fontSize = 14f,
        )
    ),
    @param:DrawableRes
    @get:DrawableRes
    @field:DrawableRes
    override val backgroundResource: Int = R.drawable.app_widget_background,
    override val fontSize: Float = 14f,
) : IWidgetData {
    override val widgetId: Int = AppWidgetManager.INVALID_APPWIDGET_ID
    override val widgetName: String = "Preview widget"
    override val hideHeader: Boolean = false
    override fun getChangeColor(
        context: Context,
        change: Float,
        changeInPercent: Float
    ): Color = if (change < 0f || changeInPercent < 0f) {
        Color.Red
    } else {
        Color.Green
    }
}

class RefreshCallback : ActionCallback {

    @Inject
    internal lateinit var stocksProvider: StocksProvider
    @Inject
    internal lateinit var appPreferences: AppPreferences
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
        coroutineScope {
            appPreferences.setRefreshing(true)
            val fetchTask = async {
                stocksProvider.fetch()
            }
            GlanceStocksWidget().updateAll(context)
            fetchTask.await()
        }
    }
}

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
        val appWidgetId = GlanceAppWidgetManager(context).getAppWidgetId(glanceId)
        val widgetData = widgetDataProvider.dataForWidgetId(appWidgetId)
        widgetData.flipChange()
        widgetDataProvider.broadcastUpdateWidget(appWidgetId)
        GlanceStocksWidget().updateAll(context)
    }
}