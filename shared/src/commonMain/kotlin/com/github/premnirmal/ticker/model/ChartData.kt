package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.components.AppNumberFormat
import com.github.premnirmal.ticker.network.data.DataPoint

/**
 * The result of a chart fetch: the previous close / current price plus the ordered list of
 * [DataPoint]s. Moved from the Android-only `HistoryProvider.ChartData` into `commonMain` so the
 * shared chart fetch ([HistoryProvider]) and the future shared presentation layer can use it. The
 * `…String()` helpers use the shared [AppNumberFormat] (replacing `AppPreferences`'s decimal
 * formats); the Compose-aware `changeColour` lives in `:app` as an extension because it depends on
 * Android theming.
 */
data class ChartData(
    val chartPreviousClose: Float,
    val regularMarketPrice: Float,
    val dataPoints: List<DataPoint>,
) {
    val change: Float
        get() = regularMarketPrice - chartPreviousClose

    val changeInPercent: Float
        get() = (regularMarketPrice - chartPreviousClose) / chartPreviousClose * 100f

    val isUp: Boolean
        get() = change > 0f

    val isDown: Boolean
        get() = change < 0f

    fun changeString(): String = AppNumberFormat.selected.format(change)

    fun changeStringWithSign(): String {
        val changeString = AppNumberFormat.selected.format(change)
        if (change >= 0) {
            return "+$changeString"
        }
        return changeString
    }

    fun changePercentString(): String =
        "${AppNumberFormat.TWO_DP.format(changeInPercent)}%"

    fun changePercentStringWithSign(): String {
        val changeString = "${AppNumberFormat.TWO_DP.format(changeInPercent)}%"
        if (changeInPercent >= 0) {
            return "+$changeString"
        }
        return changeString
    }
}
