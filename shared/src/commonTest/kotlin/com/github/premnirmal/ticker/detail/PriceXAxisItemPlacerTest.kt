package com.github.premnirmal.ticker.detail

import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.patrykandpatrick.vico.multiplatform.cartesian.CartesianDrawingContext
import com.patrykandpatrick.vico.multiplatform.cartesian.axis.HorizontalAxis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartModel
import com.patrykandpatrick.vico.multiplatform.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.multiplatform.cartesian.data.MutableCartesianChartRanges
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.CartesianLayerPadding
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.MutableCartesianLayerDimensions
import com.patrykandpatrick.vico.multiplatform.common.MutableDrawScope
import com.patrykandpatrick.vico.multiplatform.common.data.CacheStore
import com.patrykandpatrick.vico.multiplatform.common.data.ExtraStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PriceXAxisItemPlacerTest {
    private val context = object : CartesianDrawingContext {
        override val ranges = MutableCartesianChartRanges().apply {
            tryUpdate(0.0, 30.0, 1.0, 2.0, null)
        }
        override val model = CartesianChartModel(
            LineCartesianLayerModel.build { series(listOf(0, 30), listOf(1, 2)) }
        )
        override val layerDimensions = MutableCartesianLayerDimensions(xSpacing = 10f)
        override val layerBounds = Rect(0f, 0f, 300f, 200f)
        override val canvasSize = Size(300f, 200f)
        override val density = Density(1f)
        override val layoutDirection = LayoutDirection.Ltr
        override val extraStore: ExtraStore
            get() = model.extraStore
        override val cacheStore: CacheStore
            get() = error("Label placement does not use drawing caches")
        override val scrollEnabled = false
        override val zoomEnabled = false
        override val layerPadding = CartesianLayerPadding()
        override val markerX: Double? = null
        override val markerSeriesIndex: Int? = null
        override val scroll = 0f
        override val zoom = 1f
        override val fontFamilyResolver: FontFamily.Resolver
            get() = error("Label placement does not measure text directly")
        override val canvas: Canvas
            get() = error("Label placement does not draw")
        override val mutableDrawScope: MutableDrawScope
            get() = error("Label placement does not draw")
        override fun withCanvas(canvas: Canvas, block: () -> Unit) = error("Label placement does not draw")
    }

    @Test
    fun doesNotRequestEndpointPaddingUnlikeDefaultAlignedPlacer() {
        val defaultPlacer = HorizontalAxis.ItemPlacer.aligned(addExtremeLabelPadding = true)
        assertEquals(30.0, defaultPlacer.getLastLabelValue(context, 100f))

        val placer = PriceXAxisItemPlacer(spacing = 1)
        assertNull(placer.getFirstLabelValue(context, 100f))
        assertNull(placer.getLastLabelValue(context, 100f))
        assertTrue(
            placer.getWidthMeasurementLabelValues(context, context.layerDimensions, 0.0..30.0).isNotEmpty()
        )
    }

    @Test
    fun widerLabelsAreThinnedAndEndpointLabelsAreOmitted() {
        val placer = PriceXAxisItemPlacer(spacing = 1)
        val narrowLabels = placer.getLabelValues(context, 0.0..30.0, 0.0..30.0, 50f)
        val wideLabels = placer.getLabelValues(context, 0.0..30.0, 0.0..30.0, 100f)

        assertTrue(wideLabels.isNotEmpty())
        assertTrue(wideLabels.size < narrowLabels.size)
        assertTrue(wideLabels.all { it > 0.0 && it < 30.0 })
        assertTrue(wideLabels.zipWithNext().all { (left, right) ->
            (right - left) * context.layerDimensions.xSpacing >= 100f
        })
    }
}
