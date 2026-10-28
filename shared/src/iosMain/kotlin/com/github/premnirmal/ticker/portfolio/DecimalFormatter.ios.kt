package com.github.premnirmal.ticker.portfolio

import platform.Foundation.NSNumberFormatter

actual fun localeDecimalSeparator(): Char =
    NSNumberFormatter().decimalSeparator?.firstOrNull() ?: '.'
