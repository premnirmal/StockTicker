package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.premnirmal.ticker.UserPreferences
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.detail.PriceChartView
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.Range
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.Foundation.NSDate
import platform.Foundation.NSDateFormatter
import platform.Foundation.dateWithTimeIntervalSince1970

private object QuoteDetailKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
    val newsProvider: NewsProvider by inject()
    val historyProvider: HistoryProvider by inject()
    val userPreferences: UserPreferences by inject()
}

private val rangeOptions: List<Pair<Range, String>> = listOf(
    Range.ONE_DAY to "1D",
    Range.TWO_WEEKS to "2W",
    Range.ONE_MONTH to "1M",
    Range.THREE_MONTH to "3M",
    Range.ONE_YEAR to "1Y",
    Range.FIVE_YEARS to "5Y",
    Range.MAX to "Max"
)

private val PositiveColor = Color(0xFF66BB6A)
private val NegativeColor = Color(0xFFEF5350)

/**
 * iOS quote-detail content hosted by the shared `RootNavigationGraph` (the `quoteDetailContent`
 * slot). It now drives the shared [QuoteDetailViewModel] (resolved from the iOS Koin graph), so it
 * shows the live quote header plus the shared multiplatform [PriceChartView] historical price chart
 * with a range selector — the same presentation logic the Android app uses. The richer Android
 * extras (holdings editing, news list, alerts) are added as their remaining host slots are ported to
 * iOS.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteDetailScreen(
    symbol: String,
    onBack: () -> Unit
) {
    val viewModel = remember(symbol) {
        QuoteDetailViewModel(
            stocksProvider = QuoteDetailKoin.stocksProvider,
            newsProvider = QuoteDetailKoin.newsProvider,
            historyProvider = QuoteDetailKoin.historyProvider,
            userPreferences = QuoteDetailKoin.userPreferences
        )
    }

    LaunchedEffect(symbol) {
        viewModel.loadQuote(symbol)
        viewModel.fetchQuote(symbol)
        viewModel.fetchChartData(symbol, Range.ONE_DAY)
    }

    val quoteResult by viewModel.quote.collectAsState(initial = null)
    val chartData by viewModel.data.collectAsState()
    val selectedRange by viewModel.range.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()
    val quote = quoteResult?.dataSafe?.quote

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(quote?.name ?: symbol) },
                navigationIcon = {
                    TextButton(onClick = onBack) {
                        Text("Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (quote != null) {
                Text(text = quote.symbol, style = MaterialTheme.typography.titleMedium)
                Text(text = quote.priceString(), style = MaterialTheme.typography.headlineMedium)
                val isUp = quote.changeInPercent >= 0f
                Text(
                    text = "${quote.changeStringWithSign()} (${quote.changePercentStringWithSign()})",
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isUp) PositiveColor else NegativeColor
                )
            } else {
                Text(text = symbol, style = MaterialTheme.typography.titleLarge)
            }

            RangeSelector(
                selectedRange = selectedRange,
                onRangeSelected = { range -> viewModel.fetchChartData(symbol, range) }
            )

            val dataPoints = chartData?.dataPoints.orEmpty()
            when {
                dataPoints.isNotEmpty() -> {
                    val lineColor = if (chartData?.isDown == true) NegativeColor else PositiveColor
                    PriceChartView(
                        dataPoints = dataPoints,
                        lineColor = lineColor,
                        xAxisFormatter = { value -> formatAxisDate(value, selectedRange) },
                        yAxisFormatter = { value -> AppNumberFormat.selected.format(value.toFloat()) },
                        markerFormatter = { x, y ->
                            "${formatMarkerDate(x)}\n${AppNumberFormat.selected.format(y.toFloat())}"
                        },
                        modifier = Modifier.fillMaxWidth().height(260.dp)
                    )
                }
                isRefreshing -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().height(260.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                else -> {
                    Text(
                        text = "No chart data available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RangeSelector(
    selectedRange: Range,
    onRangeSelected: (Range) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        rangeOptions.forEach { (range, label) ->
            FilterChip(
                selected = range == selectedRange,
                onClick = { onRangeSelected(range) },
                label = { Text(label) }
            )
        }
    }
}

/**
 * Formats a bottom-axis value (an epoch-second timestamp) for the iOS chart. Intraday ranges show
 * the time of day; longer ranges show a short date.
 */
private fun formatAxisDate(epochSeconds: Double, range: Range): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = if (range == Range.ONE_DAY) "HH:mm" else "MMM d"
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds))
}

/** Formats the highlighted marker timestamp (epoch seconds) as a short date + time. */
private fun formatMarkerDate(epochSeconds: Double): String {
    val formatter = NSDateFormatter().apply {
        dateFormat = "MMM d, HH:mm"
    }
    return formatter.stringFromDate(NSDate.dateWithTimeIntervalSince1970(epochSeconds))
}
