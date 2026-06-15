package com.github.premnirmal.ticker.components

/**
 * Shared, multiplatform replacement for the decimal [java.text.Format]s that used to live in
 * the Android-only `AppPreferences`.
 *
 * [DEFAULT] mirrors the old `DECIMAL_FORMAT` (`#,##0.00##`) and [TWO_DP] mirrors
 * `DECIMAL_FORMAT_2DP` (`#,##0.00`). [selected] picks between them based on
 * [roundToTwoDecimalPlaces], which mirrors the `SETTING_ROUND_TWO_DP` user preference and is
 * kept in sync by `:app`'s `AppPreferences` (default `true`, matching that preference's
 * default).
 */
object AppNumberFormat {

    val DEFAULT: DecimalFormatter = DecimalFormatter(minFractionDigits = 2, maxFractionDigits = 4)
    val TWO_DP: DecimalFormatter = DecimalFormatter(minFractionDigits = 2, maxFractionDigits = 2)

    /** Mirrors `AppPreferences.roundToTwoDecimalPlaces()`; kept in sync by `:app`. */
    var roundToTwoDecimalPlaces: Boolean = true

    val selected: DecimalFormatter
        get() = if (roundToTwoDecimalPlaces) TWO_DP else DEFAULT
}
