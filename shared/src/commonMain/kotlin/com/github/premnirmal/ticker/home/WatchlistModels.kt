package com.github.premnirmal.ticker.home

import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic view of a single watchlist/widget shown as a tab in [WatchlistContent]. The
 * Android `WidgetData` is adapted to this interface by the `:app` host so the shared screen does not
 * depend on the Glance/`SharedPreferences`-backed widget model.
 */
interface WatchlistWidget {
    val name: String
    val stocks: StateFlow<List<Quote>>
    fun rearrange(tickers: List<String>)
    fun setAutoSort(autoSort: Boolean)
    fun removeStock(ticker: String)
}

/**
 * Pre-formatted total holdings / gain / loss / realized strings rendered by the total-holdings
 * popup. The locale-aware number formatting is done by the host (which owns the platform
 * `NumberFormat`). [unrealizedIsPositive]/[realizedIsPositive]/[totalIsPositive] carry the sign of
 * the underlying float so the popup can colour each value without parsing the formatted string.
 */
data class TotalGainLoss(
    val holdings: String,
    val gain: String,
    val loss: String,
    val unrealized: String,
    val unrealizedIsPositive: Boolean,
    val realized: String,
    val realizedIsPositive: Boolean,
    val total: String,
    val totalIsPositive: Boolean,
)

/**
 * Pure gain/loss/realized totals for a portfolio, before locale-aware formatting is applied by the
 * platform host (Android `HomeViewModel.totalGainLoss`, iOS `toTotalGainLoss`).
 */
data class GainLossTotals(
    val totalHoldings: Float,
    val totalGain: Float,
    val totalLoss: Float,
    val unrealized: Float,
    val realized: Float,
    val total: Float,
)

/**
 * Computes [GainLossTotals] for a portfolio. Holdings/gain/loss are summed only over quotes with an
 * open position; realized gain sums over the WHOLE portfolio, since a fully-sold symbol (no open
 * position left) still contributes its locked-in realized gain.
 */
fun List<Quote>.toGainLossTotals(): GainLossTotals {
    val withPositions = filter { it.hasPositions() }
    var totalHoldings = 0.0
    var totalGain = 0.0
    var totalLoss = 0.0
    for (quote in withPositions) {
        totalHoldings += quote.holdings().toDouble()
        val gainLoss = quote.gainLoss().toDouble()
        if (gainLoss > 0.0) {
            totalGain += gainLoss
        } else {
            totalLoss += gainLoss
        }
    }
    val unrealized = totalGain + totalLoss
    val realized = sumOf { it.realizedGain().toDouble() }
    val total = unrealized + realized
    return GainLossTotals(
        totalHoldings = totalHoldings.toFloat(),
        totalGain = totalGain.toFloat(),
        totalLoss = totalLoss.toFloat(),
        unrealized = unrealized.toFloat(),
        realized = realized.toFloat(),
        total = total.toFloat(),
    )
}
