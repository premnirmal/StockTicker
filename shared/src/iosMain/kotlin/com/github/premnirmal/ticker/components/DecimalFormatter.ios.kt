package com.github.premnirmal.ticker.components

import platform.Foundation.NSNumber
import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual class DecimalFormatter actual constructor(
    minFractionDigits: Int,
    maxFractionDigits: Int
) {

    private val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
        usesGroupingSeparator = true
        minimumFractionDigits = minFractionDigits.toULong()
        maximumFractionDigits = maxFractionDigits.toULong()
    }

    actual fun format(value: Float): String =
        formatter.stringFromNumber(NSNumber(float = value)) ?: value.toString()
}
