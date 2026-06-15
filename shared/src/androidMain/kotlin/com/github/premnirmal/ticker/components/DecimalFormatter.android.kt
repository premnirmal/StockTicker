package com.github.premnirmal.ticker.components

import java.text.DecimalFormat

actual class DecimalFormatter actual constructor(
    minFractionDigits: Int,
    maxFractionDigits: Int
) {

    private val format = DecimalFormat().apply {
        isGroupingUsed = true
        minimumFractionDigits = minFractionDigits
        maximumFractionDigits = maxFractionDigits
    }

    actual fun format(value: Float): String = format.format(value.toDouble())
}
