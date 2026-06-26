package com.github.premnirmal.ticker.portfolio

import android.icu.text.DecimalFormatSymbols

actual fun localeDecimalSeparator(): Char =
    DecimalFormatSymbols.getInstance().decimalSeparator
