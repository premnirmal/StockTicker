package com.github.premnirmal.ticker.detail

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.network.data.DataPoint
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianChartHost
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.VerticalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianValueFormatter
import com.patrykandpatrick.vico.multiplatform.cartesian.data.lineSeries
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLine
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.DefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.LineCartesianLayerMarkerTarget
import com.patrykandpatrick.vico.multiplatform.cartesian.marker.rememberDefaultCartesianMarker
import com.patrykandpatrick.vico.multiplatform.cartesian.rememberCartesianChart
import com.patrykandpatrick.vico.multiplatform.common.Fill
import com.patrykandpatrick.vico.multiplatform.common.component.rememberTextComponent

/**
 * The price chart for [com.github.premnirmal.ticker.detail.QuoteDetailScreen], rendered with Vico
 * (Compose Multiplatform). This replaces the previous Android-only MPAndroidChart `LineChart` so the
 * chart is a pure Compose composable that renders identically on Android and iOS.
 *
 * The chart draws a cubic-smoothed line of each [DataPoint]'s close value with a translucent area
 * fill underneath, a value (price) axis on the trailing edge and a date/time axis along the bottom.
 * The axis and marker label formatting is hoisted to the caller via [xAxisLabel]/[yAxisLabel]/
 * [markerLabel] (epoch-second / price -> label) so platform-specific date and number formatting
 * stays out of `commonMain`.
 */
@Composable
fun PriceChartView(
    dataPoints: List<DataPoint>,
    lineColor: Color,
    xAxisLabel: (Float) -> String,
    yAxisLabel: (Float) -> String,
    modifier: Modifier = Modifier,
    markerLabel: ((Float, Float) -> String)? = null,
) {
    val modelProducer = remember { CartesianChartModelProducer() }
    LaunchedEffect(dataPoints) {
        if (dataPoints.isEmpty()) return@LaunchedEffect
        modelProducer.runTransaction {
            lineSeries {
                series(
                    x = dataPoints.map { it.xVal },
                    y = dataPoints.map { it.closeVal },
                )
            }
        }
    }

    val line = LineCartesianLayer.rememberLine(
        fill = LineCartesianLayer.LineFill.single(Fill(lineColor)),
        areaFill = LineCartesianLayer.AreaFill.single(Fill(lineColor.copy(alpha = AREA_FILL_ALPHA))),
        pointConnector = LineCartesianLayer.PointConnector.cubic(curvature = CUBIC_CURVATURE),
    )
    val xFormatter = CartesianValueFormatter { _, value, _ -> xAxisLabel(value.toFloat()) }
    val yFormatter = CartesianValueFormatter { _, value, _ -> yAxisLabel(value.toFloat()) }
    val marker = markerLabel?.let { labelFn ->
        rememberDefaultCartesianMarker(
            label = rememberTextComponent(lineCount = 2),
            valueFormatter = DefaultCartesianMarker.ValueFormatter { _, targets ->
                val target = targets.firstOrNull() as? LineCartesianLayerMarkerTarget
                val point = target?.points?.firstOrNull()
                if (target != null && point != null) {
                    labelFn(target.x.toFloat(), point.entry.y.toFloat())
                } else {
                    ""
                }
            },
        )
    }

    val chart = rememberCartesianChart(
        rememberLineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider.series(line),
        ),
        endAxis = VerticalAxis.rememberEnd(valueFormatter = yFormatter),
        bottomAxis = HorizontalAxis.rememberBottom(
            label = rememberTextComponent(lineCount = 2),
            valueFormatter = xFormatter,
        ),
        marker = marker,
    )
    CartesianChartHost(
        chart = chart,
        modelProducer = modelProducer,
        modifier = modifier.fillMaxSize(),
    )
}

private const val AREA_FILL_ALPHA = 150f / 255f
private const val CUBIC_CURVATURE = 0.1f
