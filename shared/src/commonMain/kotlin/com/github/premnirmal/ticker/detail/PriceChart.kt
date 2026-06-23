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
                guideline = null
            ),
            marker = rememberPriceMarker(markerFormatter)
        ),
        modelProducer = modelProducer,
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
