package com.github.premnirmal.ticker.widget

import kotlinx.coroutines.flow.StateFlow

/**
 * Platform-agnostic view of a single widget's editable settings, rendered by the shared
 * [WidgetsScreen]. The Android `WidgetData` (backed by Glance/`SharedPreferences`) is adapted to this
 * interface by the `:app` host so the shared screen does not depend on the Android widget model.
 */
interface WidgetSettings {
    /** Reactive snapshot of the preference values rendered by the screen. */
    val prefs: StateFlow<WidgetPrefs>

    fun setWidgetName(value: String)
    fun setAutoSort(value: Boolean)
    fun setLayoutPref(value: Int)

    @Deprecated("will be removed in future version")
    fun setFontSize(value: Int)
    fun setWidgetSizePref(value: Int)
    fun setBgPref(value: Int)
    fun setTextColorPref(value: Int)
    fun setBoldEnabled(value: Boolean)
    fun setHideHeader(value: Boolean)
    fun setCurrencyEnabled(value: Boolean)
    fun setShowRefreshButton(value: Boolean)
}

/**
 * The subset of widget preference values rendered by [WidgetsScreen]. The Android-only fields
 * (`@DrawableRes`/`@ColorRes` resources used to actually paint the widget) stay on the Android
 * `WidgetData.Prefs` and are not exposed here.
 */
data class WidgetPrefs(
    val name: String,
    val autoSort: Boolean,
    val typePref: Int,
    @Deprecated("will be removed in future version")
    val fontSizePref: Int,
    val sizePref: Int,
    val backgroundPref: Int,
    val textColourPref: Int,
    val boldText: Boolean,
    val hideWidgetHeader: Boolean,
    val showCurrency: Boolean,
    val showRefreshButton: Boolean,
)

/**
 * The localised labels and string-array options rendered by [WidgetsScreen]. They are resolved by
 * the `:app` host (via `stringResource`/`stringArrayResource`) and passed in so the shared screen has
 * no Android resource dependency.
 */
class WidgetSettingsStrings(
    val widgetName: String,
    val addStock: String,
    val trendingStocks: String,
    val autoSort: String,
    val autoSortDesc: String,
    val layoutType: String,
    val layoutTypes: Array<String>,
    val chooseTextSize: String,
    val fontSizes: Array<String>,
    val widgetWidth: String,
    val widgetWidthTypes: Array<String>,
    val background: String,
    val backgrounds: Array<String>,
    val textColor: String,
    val textColors: Array<String>,
    val boldChange: String,
    val boldChangeDesc: String,
    val hideHeader: String,
    val hideHeaderDesc: String,
    val showCurrency: String,
    val showCurrencyDesc: String,
    val showRefresh: String,
    val showRefreshDesc: String,
)
