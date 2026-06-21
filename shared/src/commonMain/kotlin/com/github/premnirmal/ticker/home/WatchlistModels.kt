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
 * Pre-formatted total holdings / gain / loss strings rendered by the total-holdings popup. The
 * locale-aware number formatting is done by the host (which owns the platform `NumberFormat`).
 */
data class TotalGainLoss(
    val holdings: String,
    val gain: String,
    val loss: String
)
