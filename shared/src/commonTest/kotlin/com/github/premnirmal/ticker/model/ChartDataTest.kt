package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.network.data.DataPoint
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ChartDataTest {

    private var originalRounding = true

    @BeforeTest
    fun setUp() {
        originalRounding = AppNumberFormat.roundToTwoDecimalPlaces
        AppNumberFormat.roundToTwoDecimalPlaces = true
    }

    @AfterTest
    fun tearDown() {
        AppNumberFormat.roundToTwoDecimalPlaces = originalRounding
    }

    private fun chartData(
        previousClose: Float,
        price: Float
    ) = ChartData(
        chartPreviousClose = previousClose,
        regularMarketPrice = price,
        dataPoints = listOf(DataPoint(0f, price, price, price, price))
    )

    @Test
    fun change_isPriceMinusPreviousClose() {
        val data = chartData(previousClose = 10f, price = 12f)

        assertEquals(2f, data.change)
        assertTrue(data.isUp)
        assertFalse(data.isDown)
    }

    @Test
    fun changeInPercent_isRelativeToPreviousClose() {
        val data = chartData(previousClose = 10f, price = 12f)

        assertEquals(20f, data.changeInPercent)
    }

    @Test
    fun negativeChange_isDown() {
        val data = chartData(previousClose = 10f, price = 8f)

        assertEquals(-2f, data.change)
        assertFalse(data.isUp)
        assertTrue(data.isDown)
    }

    @Test
    fun changeStringWithSign_prefixesPositiveValues() {
        val up = chartData(previousClose = 10f, price = 12f)

        assertTrue(up.changeStringWithSign().startsWith("+"))
        assertFalse(up.changeString().startsWith("+"))
    }

    @Test
    fun changeStringWithSign_keepsNegativeSignWithoutPlus() {
        val down = chartData(previousClose = 10f, price = 8f)

        assertFalse(down.changeStringWithSign().startsWith("+"))
        assertTrue(down.changeStringWithSign().startsWith("-"))
    }

    @Test
    fun changePercentString_isSuffixedWithPercent() {
        val up = chartData(previousClose = 10f, price = 12f)

        assertTrue(up.changePercentString().endsWith("%"))
        assertTrue(up.changePercentStringWithSign().startsWith("+"))
        assertTrue(up.changePercentStringWithSign().endsWith("%"))
    }
}
