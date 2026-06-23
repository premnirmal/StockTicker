package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.premnirmal.ticker.network.data.DataPoint
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.Zoom
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberVicoZoomState
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.Axis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.rememberAxisGuidelineComponent
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.CartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.Insets
import com.patrykandpatrick.vico.multiplatform.common.MarkerCornerBasedShape
import com.patrykandpatrick.vico.multiplatform.common.component.TextComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberShapeComponent
import com.patrykandpatrick.vico.multiplatform.common.component.rememberTextComponent
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore

private const val AXIS_LABEL_COUNT = 5

/**
 * The approximate number of labels to show along the time (bottom) axis. Without a cap, Vico's
 * aligned placer puts a label at every "step" along the axis, so a long range (3M, 1Y, ...) renders
 * dozens of overlapping labels that collapse into an unreadable smear. Spacing the labels to hit
 * roughly this count keeps them legible regardless of how many points/steps there are.
 */
internal const val X_AXIS_LABEL_COUNT = 4

// Vico rounds the GCD of the x-value deltas to this many decimals when deriving the axis step; see
// `Double.gcdWith` in Vico (DOUBLE_GCD_DECIMALS). We replicate it so [xAxisLabelSpacing] computes the
// same number of axis steps Vico will, and therefore the same number of labels.
private const val DOUBLE_GCD_DECIMALS = 4

/**
 * Fraction of the data's value span added as padding above the highest and below the lowest price so
 * the line/area has a little breathing room from the chart edges.
 */
private const val Y_RANGE_PADDING_FRACTION = 0.1

/**
 * A [CartesianLayerRangeProvider] that scales the value axis to the data's own min/max (with a small
 * padding) instead of Vico's default behaviour of anchoring the minimum at zero. Stock prices are
 * always positive, so the default provider would compress the whole price line into a thin, flat
 * band near the top of a `0..max` axis; fitting the range to the data restores the visible movement.
 */
internal object PriceRangeProvider : CartesianLayerRangeProvider {
    override fun getMinY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        minY - padding(minY, maxY)

    override fun getMaxY(minY: Double, maxY: Double, extraStore: ExtraStore): Double =
        maxY + padding(minY, maxY)

    /**
     * The amount to extend the range beyond [minY]/[maxY]. For a non-flat series this is a fraction
     * of the value span; for flat data (a single value or constant price) the span is zero, so fall
     * back to a fraction of the value itself (or 1.0) to avoid a zero-height axis.
     */
    private fun padding(minY: Double, maxY: Double): Double {
        val span = maxY - minY
        if (span > 0.0) return span * Y_RANGE_PADDING_FRACTION
        val magnitude = maxOf(kotlin.math.abs(minY), kotlin.math.abs(maxY))
        return if (magnitude > 0.0) magnitude * Y_RANGE_PADDING_FRACTION else 1.0
    }
}

/**
 * The number of axis "steps" between consecutive bottom-axis labels needed to keep the visible label
 * count around [X_AXIS_LABEL_COUNT].
 *
 * Vico's `aligned` item placer positions labels every `spacing` axis steps, where one step is the
 * axis's `xStep` — the greatest common divisor of the gaps between consecutive x-values (see
 * [xDeltaGcd]). The total number of steps spanning the data is therefore `(maxX - minX) / xStep`,
 * which is **not** the same as the point count: weekend/holiday gaps and the limited precision of the
 * `Float` epoch-second timestamps make the GCD small, so the step count can be far larger (e.g. ~350
 * for a year of daily data). Spacing the labels by `ceil(steps / X_AXIS_LABEL_COUNT)` (at least 1)
 * caps them at roughly the target count for any range, fixing the unreadably granular axis that
 * results from labelling every step.
 *
 * @param xValues the chart's x-values (epoch seconds), as passed to Vico.
 */
internal fun xAxisLabelSpacing(xValues: List<Double>): Int {
    if (xValues.size <= X_AXIS_LABEL_COUNT) return 1
    val step = xDeltaGcd(xValues)
    val span = xValues.last() - xValues.first()
    if (step <= 0.0 || span <= 0.0) return 1
    val steps = (span / step).roundToInt()
    if (steps <= X_AXIS_LABEL_COUNT) return 1
    return (steps + X_AXIS_LABEL_COUNT - 1) / X_AXIS_LABEL_COUNT
}

/**
 * The greatest common divisor of the differences between consecutive [xValues], matching Vico's
 * `getXDeltaGcd`/`Double.gcdWith` so [xAxisLabelSpacing] derives the same axis step Vico uses.
 */
private fun xDeltaGcd(xValues: List<Double>): Double {
    var gcd: Double? = null
    for (i in 1 until xValues.size) {
        val delta = abs(xValues[i] - xValues[i - 1])
        if (delta == 0.0) continue
        gcd = if (gcd == null) delta else gcd.gcdWith(delta)
    }
    return gcd ?: 1.0
}

/** Mirrors Vico's `Double.gcdWith`: a Euclidean GCD rounded to [DOUBLE_GCD_DECIMALS] decimals. */
private fun Double.gcdWith(other: Double): Double =
    gcdWithImpl(this, other, threshold = 10.0.pow(-DOUBLE_GCD_DECIMALS - 1))
        .roundToDecimals(DOUBLE_GCD_DECIMALS)

private tailrec fun gcdWithImpl(a: Double, b: Double, threshold: Double): Double =
    when {
        a < b -> gcdWithImpl(b, a, threshold)
        abs(b) < threshold -> a
        else -> gcdWithImpl(b, a - floor(a / b) * b, threshold)
    }

private fun Double.roundToDecimals(decimals: Int): Double {
    val multiplier = 10.0.pow(decimals)
    return (this * multiplier).roundToLong() / multiplier
}

/**
 * Shared, multiplatform price chart rendered with Vico (replacing the Android-only MPAndroidChart
 * `LineChart`). It plots the closing price of each [DataPoint] over time as a smoothed, area-filled
 * line with a value axis on the trailing edge and a time axis along the bottom.
 *
 * Date/number formatting is platform-specific (the Android app formats with `java.time` and the
 * user's locale/preferences), so the axis and marker label formatters are supplied by the host.
 *
 * @param xAxisFormatter formats a bottom-axis value (an epoch-second timestamp) into a label.
 * @param yAxisFormatter formats a trailing-axis value (a price) into a label.
 * @param markerFormatter formats the highlighted point (epoch-second timestamp, price) into a label.
 */
@Composable
fun PriceChartView(
    dataPoints: List<DataPoint>,
    lineColor: Color,
    xAxisFormatter: (Double) -> String,
    yAxisFormatter: (Double) -> String,
    markerFormatter: (x: Double, y: Double) -> String,
    modifier: Modifier = Modifier
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dataPoints) {
        if (dataPoints.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = dataPoints.map { it.xVal },
                    y = dataPoints.map { it.closeVal }
                )
            }
        }
    }

    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
        areaFill = LineCartesianLayer.AreaFill.single(Fill(lineColor.copy(alpha = 0.4f))),
        pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = 0.1f)
    )

    CartesianChartHost(
        chart = rememberCartesianChart(
            rememberLineCartesianLayer(
                lineProvider = LineCartesianLayer.LineProvider.series(line),
                rangeProvider = PriceRangeProvider,
                verticalAxisPosition = Axis.Position.Vertical.End
            ),
            endAxis = VerticalAxis.rememberEnd(
                valueFormatter = remember(yAxisFormatter) {
                    CartesianValueFormatter { _, value, _ -> yAxisFormatter(value) }
                },
                itemPlacer = remember { VerticalAxis.ItemPlacer.count({ AXIS_LABEL_COUNT }) },
                guideline = null
            ),
            bottomAxis = HorizontalAxis.rememberBottom(
                valueFormatter = remember(xAxisFormatter) {
                    CartesianValueFormatter { _, value, _ -> xAxisFormatter(value) }
                },
                // By default the horizontal axis reserves half a label's width of in-layer padding at
                // each end so the first/last labels aren't clipped, which leaves an empty gap on the
                // left and right of the line. Disabling addExtremeLabelPadding removes that gap so the
                // line spans the full chart width; the axis still reserves outer layer margin for the
                // extreme labels (getStartLayerMargin/getEndLayerMargin), so they stay fully visible.
                //
                // The aligned placer otherwise labels every axis step, and the axis step (the GCD of
                // the timestamp gaps) is small because of weekend/holiday gaps and the limited
                // precision of the Float timestamps — so a long range produces hundreds of labels
                // that collapse into an unreadable smear. Spacing the labels by xAxisLabelSpacing
                // caps the visible labels at roughly X_AXIS_LABEL_COUNT so they stay legible.
                itemPlacer = remember(dataPoints) {
                    val spacing = xAxisLabelSpacing(dataPoints.map { it.xVal.toDouble() })
                    HorizontalAxis.ItemPlacer.aligned(
                        spacing = { spacing },
                        addExtremeLabelPadding = false
                    )
                },
                guideline = null
            ),
            marker = rememberPriceMarker(markerFormatter)
        ),
        modelProducer = modelProducer,
        // Vico's default initial zoom (Zoom.max(Zoom.fixed(), Zoom.Content)) zooms in to a fixed
        // per-point spacing and scrolls to the latest point, so for long ranges (3M, 1Y, ...) only
        // the most recent handful of points is visible while the Y-axis still fits the whole series.
        // That squeezes the visible line into a thin, flat band. Zoom.Content fits every point to the
        // chart width so the full range — and its smaller fluctuations — is visible at a glance.
        zoomState = rememberVicoZoomState(initialZoom = remember { Zoom.Content }),
        modifier = modifier
    )
}

@Composable
private fun rememberPriceMarker(
    markerFormatter: (x: Double, y: Double) -> String
): CartesianMarker {
    val valueFormatter = remember(markerFormatter) {
        DefaultCartesianMarker.ValueFormatter { _, targets ->
            val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
            val point = target?.points?.firstOrNull()
            if (target == null || point == null) "" else markerFormatter(target.x, point.entry.y)
        }
    }
    val labelBackground = rememberShapeComponent(
        fill = Fill(MaterialTheme.colorScheme.surface),
        shape = MarkerCornerBasedShape(CircleShape),
        strokeFill = Fill(MaterialTheme.colorScheme.outline),
        strokeThickness = 1.dp
    )
    val label = rememberTextComponent(
        style = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
            fontSize = 12.sp
        ),
        padding = Insets(8.dp, 4.dp),
        background = labelBackground,
        minWidth = TextComponent.MinWidth.fixed(40.dp)
    )
    return rememberDefaultCartesianMarker(
        label = label,
        valueFormatter = valueFormatter,
        guideline = rememberAxisGuidelineComponent()
    )
}
