package com.github.premnirmal.ticker

import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.settings.PreferenceStore
import java.text.DecimalFormat
import java.text.Format
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle.MEDIUM

/**
 * Android entry point for the shared [UserPreferences] settings contract and [CrumbStore] crumb
 * persistence, backed by a [PreferenceStore].
 *
 * The read/write logic itself is fully shared in [SharedUserPreferences]; this class only adds the
 * Android-only extras that depend on JVM types — the `java.text` decimal formatters
 * ([selectedDecimalFormat]) and the saved app-version bookkeeping — plus the legacy preference-key
 * and widget-related constants that the rest of the `:app` module still references.
 *
 * Created by premnirmal on 2/26/16.
 */
class AppPreferences constructor(
    store: PreferenceStore,
) : SharedUserPreferences(store) {

    init {
        INSTANCE = this
    }

    fun getLastSavedVersionCode(): Int = store.getInt(APP_VERSION_CODE, -1)
    fun saveVersionCode(code: Int) {
        store.setInt(APP_VERSION_CODE, code)
    }

    val selectedDecimalFormat: Format
        get() = if (roundToTwoDecimalPlaces()) {
            DECIMAL_FORMAT_2DP
        } else {
            DECIMAL_FORMAT
        }

    companion object {

        private lateinit var INSTANCE: AppPreferences

        fun List<String>.toCommaSeparatedString(): String {
            val builder = StringBuilder()
            for (string in this) {
                builder.append(string)
                builder.append(",")
            }
            val length = builder.length
            if (length > 1) {
                builder.deleteCharAt(length - 1)
            }
            return builder.toString()
        }

        const val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
        const val PREFS_NAME = "com.github.premnirmal.ticker"
        const val START_TIME = "START_TIME"
        const val END_TIME = "END_TIME"
        const val UPDATE_DAYS = "UPDATE_DAYS"
        const val TUTORIAL_SHOWN = "TUTORIAL_SHOWN"
        const val SETTING_AUTOSORT = "SETTING_AUTOSORT"
        const val SETTING_HIDE_HEADER = "SETTING_HIDE_HEADER"
        const val SETTING_ROUND_TWO_DP = "SETTING_ROUND_TWO_DP"
        const val SETTING_NOTIFICATION_ALERTS = "SETTING_NOTIFICATION_ALERTS"
        const val PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP = "PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP"

        const val WIDGET_BG = "WIDGET_BG"
        const val WIDGET_REFRESHING = "WIDGET_REFRESHING"
        const val TEXT_COLOR = "TEXT_COLOR"
        const val UPDATE_INTERVAL = "UPDATE_INTERVAL"
        const val LAYOUT_TYPE = "LAYOUT_TYPE"
        const val WIDGET_SIZE = "WIDGET_SIZE"

        @Deprecated("will be removed in future version")
        const val FONT_SIZE = "FONT_SIZE"
        const val BOLD_CHANGE = "BOLD_CHANGE"
        const val SHOW_CURRENCY = "SHOW_CURRENCY"
        const val SHOW_REFRESH = "SHOW_REFRESH"
        const val PERCENT = "PERCENT"
        const val CRUMB = "CRUMB"
        const val APP_VERSION_CODE = "APP_VERSION_CODE"
        const val APP_THEME = "APP_THEME"
        const val SYSTEM = 0
        const val TRANSPARENT = 1
        const val TRANSLUCENT = 2
        const val LIGHT = 1
        const val DARK = 2
        const val LIGHT_THEME = 0
        const val DARK_THEME = 1
        const val FOLLOW_SYSTEM_THEME = 2

        val TIME_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm")
        val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofLocalizedDate(MEDIUM)
        val AXIS_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("LLL dd-yyyy")

        val DECIMAL_FORMAT: Format = DecimalFormat("#,##0.00##")
        val DECIMAL_FORMAT_2DP: Format = DecimalFormat("#,##0.00")

        val SELECTED_DECIMAL_FORMAT: Format
            get() = if (::INSTANCE.isInitialized) { INSTANCE.selectedDecimalFormat } else DECIMAL_FORMAT

        val SELECTED_THEME: SelectedTheme
            get() = if (::INSTANCE.isInitialized) { INSTANCE.selectedTheme } else SelectedTheme.SYSTEM
    }
}
