package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.premnirmal.ticker.home.HomeActivity
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import com.github.premnirmal.ticker.portfolio.PortfolioSummaryCalculator
import com.github.premnirmal.ticker.portfolio.StockPerformance
import com.github.premnirmal.tickerwidget.R
import androidx.compose.foundation.layout.Column as ComposeColumn
import androidx.compose.foundation.layout.Row as ComposeRow
import androidx.compose.foundation.layout.fillMaxWidth as composeFillMaxWidth
import androidx.compose.foundation.layout.padding as composePadding
import androidx.compose.material3.Text as ComposeText
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.text.FontWeight as GlanceFontWeight
import androidx.glance.text.Text as GlanceText
import androidx.glance.text.TextAlign as GlanceTextAlign

internal enum class StockTableColumn(val label: Int) {
    Stock(R.string.stock_table_stock),
    Holdings(R.string.portfolio_holdings),
    Day(R.string.stock_table_day),
    Gain(R.string.portfolio_gain),
    GainPercent(R.string.portfolio_gain_percent),
    Month(R.string.stock_table_month),
}

/** Width is independent of the existing preference for the number of stock cards per row. */
internal fun stockTableColumns(widthDp: Float, screenWidthDp: Int): List<StockTableColumn> {
    val width = widthDp * 100
    val screen = screenWidthDp.coerceAtLeast(1)
    return StockTableColumn.entries.filter { column ->
        when (column) {
            StockTableColumn.Stock, StockTableColumn.Day -> true
            StockTableColumn.Holdings -> width >= screen * 45f
            StockTableColumn.Gain -> width >= screen * 60f
            StockTableColumn.GainPercent -> width >= screen * 75f
            StockTableColumn.Month -> width >= screen * 85f
        }
    }
}

internal fun stockTableScreenWidth(context: Context): Int {
    val resources = context.resources
    return resources.configuration.screenWidthDp.takeIf { it > 0 }
        ?: (resources.displayMetrics.widthPixels / resources.displayMetrics.density).toInt().coerceAtLeast(1)
}

internal data class StockTableRow(
    val symbol: String,
    val name: String,
    val currency: String,
    val metrics: Map<StockTableColumn, SummaryMetric>,
)

/** Quote ordering and display names remain authoritative, including when restoring older state. */
internal fun stockTableRows(
    context: Context,
    quotes: List<Quote>,
    performance: List<StockPerformance>,
): List<StockTableRow> {
    val stored = performance.associateBy { it.symbol }
    val fallback = PortfolioSummaryCalculator.calculateStocks(quotes).associateBy { it.symbol }
    return quotes.distinctBy { it.symbol }.map { quote ->
        val stock = stored[quote.symbol] ?: fallback.getValue(quote.symbol)
        val metrics = summaryMetrics(
            context,
            PortfolioSummary(
                stock.currency,
                stock.holdings,
                stock.gain,
                stock.gainPercent,
                stock.dayChangePercent,
                stock.monthChangePercent,
            ),
        )
        StockTableRow(
            symbol = quote.symbol,
            name = quote.properties?.displayname?.takeIf { it.isNotBlank() } ?: quote.symbol,
            currency = stock.currency,
            metrics = mapOf(
                StockTableColumn.Holdings to metrics[0],
                StockTableColumn.Day to metrics[1],
                StockTableColumn.Gain to metrics[3],
                StockTableColumn.GainPercent to metrics[4],
                StockTableColumn.Month to metrics[2],
            ),
        )
    }
}

@Composable
internal fun StockTableHeaderGlance(
    context: Context,
    state: SerializableWidgetState,
    columns: List<StockTableColumn>,
) {
    GlanceRow(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp)) {
        columns.forEach { column ->
            GlanceText(
                text = context.getString(column.label),
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                style = TextStyle(
                    color = ColorProvider(state.textColor),
                    fontSize = state.fontSize.sp,
                    fontWeight = GlanceFontWeight.Bold,
                    textAlign = if (column == StockTableColumn.Stock) GlanceTextAlign.Start else GlanceTextAlign.End,
                ),
            )
        }
    }
}

@Composable
internal fun StockTableRowGlance(
    state: SerializableWidgetState,
    columns: List<StockTableColumn>,
    row: StockTableRow,
) {
    GlanceRow(
        modifier = GlanceModifier.fillMaxWidth().padding(vertical = 3.dp).clickable(
            actionStartActivity<HomeActivity>(
                actionParametersOf(ActionParameters.Key<String>(HomeActivity.EXTRA_SYMBOL) to row.symbol),
            ),
        ),
    ) {
        GlanceColumn(modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp)) {
            GlanceText(
                text = row.name,
                style = TextStyle(color = ColorProvider(state.textColor), fontSize = state.fontSize.sp),
                maxLines = 2,
            )
            GlanceText(
                text = row.currency,
                style = TextStyle(color = ColorProvider(state.textColor), fontSize = (state.fontSize * 0.85f).sp),
            )
        }
        columns.drop(1).forEach { column ->
            val metric = row.metrics.getValue(column)
            GlanceText(
                text = metric.value,
                modifier = GlanceModifier.defaultWeight().padding(horizontal = 2.dp),
                style = TextStyle(
                    color = ColorProvider(metricColor(state, metric.change)),
                    fontSize = state.fontSize.sp,
                    textAlign = GlanceTextAlign.End,
                ),
            )
        }
    }
}

@Composable
internal fun StockTableHeaderPreview(
    context: Context,
    state: SerializableWidgetState,
    columns: List<StockTableColumn>,
) {
    ComposeRow(modifier = Modifier.composeFillMaxWidth().composePadding(vertical = 3.dp)) {
        columns.forEach { column ->
            ComposeText(
                text = context.getString(column.label),
                modifier = Modifier.weight(1f).composePadding(horizontal = 2.dp),
                color = colorResource(state.textColor),
                fontSize = state.fontSize.sp,
                fontWeight = FontWeight.Bold,
                textAlign = if (column == StockTableColumn.Stock) TextAlign.Start else TextAlign.End,
            )
        }
    }
}

@Composable
internal fun StockTableRowPreview(
    state: SerializableWidgetState,
    columns: List<StockTableColumn>,
    row: StockTableRow,
) {
    ComposeRow(modifier = Modifier.composeFillMaxWidth().composePadding(vertical = 3.dp)) {
        ComposeColumn(modifier = Modifier.weight(1f).composePadding(horizontal = 2.dp)) {
            ComposeText(row.name, color = colorResource(state.textColor), fontSize = state.fontSize.sp, maxLines = 2)
            ComposeText(row.currency, color = colorResource(state.textColor), fontSize = (state.fontSize * 0.85f).sp)
        }
        columns.drop(1).forEach { column ->
            val metric = row.metrics.getValue(column)
            ComposeText(
                text = metric.value,
                modifier = Modifier.weight(1f).composePadding(horizontal = 2.dp),
                color = colorResource(metricColor(state, metric.change)),
                fontSize = state.fontSize.sp,
                textAlign = TextAlign.End,
            )
        }
    }
}
