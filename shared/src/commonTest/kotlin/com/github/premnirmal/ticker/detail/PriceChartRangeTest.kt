package com.github.premnirmal.ticker.detail

import com.patrykandpatrick.vico.multiplatform.cartesian.axis.Axis
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianLayerRangeProvider
import com.patrykandpatrick.vico.multiplatform.cartesian.data.CartesianChartRanges
import com.patrykandpatrick.vico.multiplatform.cartesian.data.LineCartesianLayerModel
import com.patrykandpatrick.vico.multiplatform.cartesian.data.MutableCartesianChartRanges
import com.patrykandpatrick.vico.multiplatform.cartesian.layer.LineCartesianLayer
import kotlin.test.Test
import kotlin.test.assertTrue

class PriceChartRangeTest {

    private fun rangesFor(
        rangeProvider: CartesianLayerRangeProvider,
        ys: List<Double>,
        verticalAxisPosition: Axis.Position.Vertical? = null
    ): CartesianChartRanges {
        val model = LineCartesianLayerModel.build { series(ys.indices.toList(), ys) }
        // updateChartRanges only consults the model + range provider, never the line itself, so a
        // stub LineProvider avoids constructing a Line (which would touch android.graphics.Paint).
        val layer = LineCartesianLayer(
            lineProvider = LineCartesianLayer.LineProvider { _, _ -> error("not needed") },
            rangeProvider = rangeProvider,
            verticalAxisPosition = verticalAxisPosition
        )
        val ranges = MutableCartesianChartRanges()
        layer.updateChartRanges(ranges, model)
        return ranges
    }

    private fun rangeFor(
        rangeProvider: CartesianLayerRangeProvider,
        ys: List<Double>
    ): CartesianChartRanges.YRange = rangesFor(rangeProvider, ys).getYRange(null)

    @Test
    fun defaultProviderAnchorsPositiveDataToZero() {
        // Documents the behaviour the custom provider exists to avoid.
        val range = rangeFor(CartesianLayerRangeProvider.auto(), listOf(362.0, 364.0, 366.0, 367.46))
        assertTrue(range.minY == 0.0, "Default auto() provider should anchor minY at 0 but was ${range.minY}")
    }

    @Test
    fun priceProviderFitsAxisToData() {
        val range = rangeFor(PriceRangeProvider, listOf(362.0, 364.0, 366.0, 367.46))
        assertTrue(
            range.minY > 300.0,
            "PriceRangeProvider should fit minY close to the data minimum but was ${range.minY}"
        )
        assertTrue(
            range.maxY < 400.0,
            "PriceRangeProvider should fit maxY close to the data maximum but was ${range.maxY}"
        )
    }

    @Test
    fun priceProviderFitsTheEndAxisRange() {
        // PriceChartView associates its line layer with the end VerticalAxis, so the fitted range
        // must be available for the end axis position.
        val ranges = rangesFor(
            PriceRangeProvider,
            listOf(362.0, 364.0, 366.0, 367.46),
            verticalAxisPosition = Axis.Position.Vertical.End
        )
        val endRange = ranges.getYRange(Axis.Position.Vertical.End)
        assertTrue(
            endRange.minY > 300.0 && endRange.maxY < 400.0,
            "End-axis range should be fitted to the data but was ${endRange.minY}..${endRange.maxY}"
        )
    }
}
