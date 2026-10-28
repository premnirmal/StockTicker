package com.github.premnirmal.ticker.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.portfolio.PortfolioSummary
import com.github.premnirmal.tickerwidget.R
import java.text.NumberFormat
import androidx.compose.foundation.layout.Column as ComposeColumn
import androidx.compose.foundation.layout.Row as ComposeRow
import androidx.compose.foundation.layout.fillMaxWidth as composeFillMaxWidth
import androidx.compose.foundation.layout.padding as composePadding
import androidx.compose.material3.Text as ComposeText
import androidx.glance.layout.Column as GlanceColumn
import androidx.glance.layout.Row as GlanceRow
import androidx.glance.text.Text as GlanceText

internal data class SummaryMetric(val label: String, val value: String, val change: Double?)

internal fun widgetColumnCount(widthDp: Int, sizePref: Int): Int = when {
    sizePref > 0 -> sizePref
    widthDp > 850 -> 4
    widthDp > 750 -> 3
    widthDp > 250 -> 2
    else -> 1
}

/** Both renderers use the same labels, values, ordering and availability rules. */
internal fun summaryMetrics(context: Context, summary: PortfolioSummary): List<SummaryMetric> {
    val moneyFormat = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = if (AppNumberFormat.roundToTwoDecimalPlaces) 2 else 4
    }
    val percentFormat = NumberFormat.getNumberInstance().apply {
        minimumFractionDigits = 2
        maximumFractionDigits = 2
    }
    fun metric(label: Int, value: Double?, percent: Boolean = false, signed: Boolean = true): SummaryMetric {
        val formatted = value?.takeIf { it.isFinite() }?.let {
            val number = if (percent) percentFormat.format(it) else moneyFormat.format(it)
            (if (signed && it > 0) "+" else "") + number + (if (percent) "%" else "")
        } ?: "—"
        return SummaryMetric(context.getString(label), formatted, value?.takeIf { signed && it.isFinite() })
    }
    return listOf(
        metric(R.string.portfolio_holdings, summary.holdings, signed = false),
        metric(R.string.portfolio_day_change, summary.dayChangePercent, percent = true),
        metric(R.string.portfolio_month_change, summary.monthChangePercent, percent = true),
        metric(R.string.portfolio_gain, summary.gain),
        metric(R.string.portfolio_gain_percent, summary.gainPercent, percent = true),
    )
}

private fun metricColor(state: SerializableWidgetState, change: Double?): Int = when {
    change == null || change == 0.0 -> state.textColor
    change < 0 -> state.negativeTextColor
    else -> state.positiveTextColor
}

@Composable
internal fun PortfolioSummaryGlance(state: SerializableWidgetState, summaries: List<PortfolioSummary>) {
    val context = LocalContext.current
    GlanceColumn(modifier = GlanceModifier.fillMaxWidth().padding(vertical = 4.dp)) {
        if (summaries.isEmpty()) {
            GlanceText(
                context.getString(R.string.portfolio_no_holdings),
                style = TextStyle(color = ColorProvider(state.textColor), fontSize = state.fontSize.sp),
            )
        }
        summaries.forEach { summary ->
            val metrics = summaryMetrics(context, summary)
            GlanceText(
                context.getString(R.string.portfolio_currency, summary.currency),
                style = TextStyle(color = ColorProvider(state.textColor), fontSize = state.fontSize.sp),
            )
            listOf(metrics.take(1)).plus(metrics.drop(1).chunked(2)).forEach { row ->
                GlanceRow(modifier = GlanceModifier.fillMaxWidth()) {
                    row.forEach { metric ->
                        GlanceColumn(modifier = GlanceModifier.defaultWeight().padding(end = 4.dp, bottom = 3.dp)) {
                            GlanceText(
                                metric.label,
                                style = TextStyle(color = ColorProvider(state.textColor), fontSize = state.fontSize.sp),
                            )
                            GlanceText(
                                metric.value,
                                style = TextStyle(
                                    color = ColorProvider(metricColor(state, metric.change)),
                                    fontSize = state.fontSize.sp,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
internal fun PortfolioSummaryPreview(state: SerializableWidgetState, summaries: List<PortfolioSummary>) {
    val context = androidx.compose.ui.platform.LocalContext.current
    ComposeColumn(modifier = Modifier.composeFillMaxWidth().composePadding(vertical = 4.dp)) {
        if (summaries.isEmpty()) {
            ComposeText(
                stringResource(R.string.portfolio_no_holdings),
                color = colorResource(state.textColor),
                fontSize = state.fontSize.sp,
            )
        }
        summaries.forEach { summary ->
            val metrics = summaryMetrics(context, summary)
            ComposeText(
                stringResource(R.string.portfolio_currency, summary.currency),
                color = colorResource(state.textColor),
                fontSize = state.fontSize.sp,
            )
            listOf(metrics.take(1)).plus(metrics.drop(1).chunked(2)).forEach { row ->
                ComposeRow(modifier = Modifier.composeFillMaxWidth()) {
                    row.forEach { metric ->
                        ComposeColumn(modifier = Modifier.weight(1f).composePadding(end = 4.dp, bottom = 3.dp)) {
                            ComposeText(
                                metric.label,
                                color = colorResource(state.textColor),
                                fontSize = state.fontSize.sp,
                            )
                            ComposeText(
                                metric.value,
                                color = colorResource(metricColor(state, metric.change)),
                                fontSize = state.fontSize.sp,
                            )
                        }
                    }
                }
            }
        }
    }
}
