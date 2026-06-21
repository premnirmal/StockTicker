package com.github.premnirmal.ticker.portfolio

import platform.Foundation.NSNumberFormatter
import platform.Foundation.NSNumberFormatterDecimalStyle

actual fun localeDecimalSeparator(): Char {
    val formatter = NSNumberFormatter().apply {
        numberStyle = NSNumberFormatterDecimalStyle
    }
    return formatter.decimalSeparator.firstOrNull() ?: '.'
}
