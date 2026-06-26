package com.github.premnirmal.ticker.components

import kotlin.math.abs

/**
 * Formats a price into a short, "compact" label for chart axes. Full prices like `52,682.975`
 * are far too wide for a value axis, so this abbreviates large magnitudes with a `K`/`M`/`B`/`T`
 * suffix and keeps just enough precision to stay readable. The result is kept to roughly 4-5
 * characters (plus a sign):
 *
 * - `52682.975` -> `52.7K`
 * - `1234567.0` -> `1.23M`
 * - `152.34`    -> `152.3`
 * - `52.18`     -> `52.18`
 *
 * Values below 1000 keep more precision (smaller charts often span a narrow price band) while
 * larger magnitudes are scaled down so neighbouring axis labels stay distinct without overflowing.
 */
object CompactNumberFormat {

    private const val THOUSAND = 1_000.0
    private const val MILLION = 1_000_000.0
    private const val BILLION = 1_000_000_000.0
    private const val TRILLION = 1_000_000_000_000.0

    // Reused formatters keyed by their maximum number of fraction digits. minFractionDigits is 0 so
    // trailing zeros are trimmed, keeping labels as short as possible (e.g. "52K" instead of "52.0K").
    private val zeroDp = DecimalFormatter(minFractionDigits = 0, maxFractionDigits = 0)
    private val oneDp = DecimalFormatter(minFractionDigits = 0, maxFractionDigits = 1)
    private val twoDp = DecimalFormatter(minFractionDigits = 0, maxFractionDigits = 2)

    fun format(value: Double): String {
        val magnitude = abs(value)
        val sign = if (value < 0) "-" else ""
        return when {
            magnitude >= TRILLION -> sign + scaled(magnitude / TRILLION, "T")
            magnitude >= BILLION -> sign + scaled(magnitude / BILLION, "B")
            magnitude >= MILLION -> sign + scaled(magnitude / MILLION, "M")
            magnitude >= THOUSAND -> sign + scaled(magnitude / THOUSAND, "K")
            else -> sign + small(magnitude)
        }
    }

    /** Formats a scaled (0..1000) magnitude with a unit suffix, using fewer decimals as it grows. */
    private fun scaled(value: Double, suffix: String): String {
        val formatter = when {
            value < 10.0 -> twoDp
            value < 100.0 -> oneDp
            else -> zeroDp
        }
        return formatter.format(value.toFloat()) + suffix
    }

    /** Formats a sub-1000 magnitude, keeping more precision for the narrow price bands of small charts. */
    private fun small(value: Double): String {
        val formatter = if (value >= 100.0) oneDp else twoDp
        return formatter.format(value.toFloat())
    }
}
