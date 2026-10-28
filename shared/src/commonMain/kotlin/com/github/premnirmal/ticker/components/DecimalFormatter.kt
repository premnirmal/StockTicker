package com.github.premnirmal.ticker.components

/**
 * Multiplatform grouped decimal formatter.
 *
 * Formats a [Float] using locale-aware grouping and decimal separators with a configurable
 * number of fraction digits, replacing the Android-only `java.text.DecimalFormat`. The
 * [minFractionDigits]/[maxFractionDigits] pair maps to the previous `DecimalFormat` patterns:
 * `#,##0.00##` is `(2, 4)` and `#,##0.00` is `(2, 2)`.
 *
 * On Android this is backed by `java.text.DecimalFormat`; on iOS by `NSNumberFormatter`. Both
 * use the default locale's separators and half-even rounding, so output is identical to the
 * previous Android behaviour.
 */
expect class DecimalFormatter(minFractionDigits: Int, maxFractionDigits: Int) {
    fun format(value: Float): String
}
