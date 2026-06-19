package com.github.premnirmal.ticker.widget

import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-neutral contract for a single widget's stock list and the data operations that shared
 * presentation logic (Phase 3 ViewModels) performs on it. The Android implementation
 * ([com.github.premnirmal.ticker.widget.WidgetData]) keeps the Glance/`SharedPreferences`-backed UI
 * state (colors, layout rendering, `Parcelable`) that has no cross-platform equivalent; iOS provides
 * its own implementation later.
 */
interface IWidgetData {
    val widgetId: Int
    val widgetName: String

    val stocks: StateFlow<List<Quote>>

    val changeType: ChangeType

    val layoutType: LayoutType

    fun hasTicker(symbol: String): Boolean

    fun addTicker(ticker: String)

    fun addTickers(tickers: List<String>)

    fun removeStock(ticker: String)

    fun rearrange(tickers: List<String>)

    fun autoSortEnabled(): Boolean

    fun setAutoSort(autoSort: Boolean)

    fun addAllFromStocksProvider()

    enum class ChangeType {
        Value,
        Percent,
    }

    enum class LayoutType {
        Animated,
        Tabs,
        Fixed,
        MyPortfolio;

        companion object {
            fun fromInt(value: Int): LayoutType {
                return when (value) {
                    0 -> Animated
                    1 -> Tabs
                    2 -> Fixed
                    3 -> MyPortfolio
                    else -> Animated
                }
            }
        }
    }

    enum class BackgroundType {
        System,
        Transparent,
        Translucent,
    }

    enum class TextColorType {
        System,
        Dark,
        Light,
    }
}
