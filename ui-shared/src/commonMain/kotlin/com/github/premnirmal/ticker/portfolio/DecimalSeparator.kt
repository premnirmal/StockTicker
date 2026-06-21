package com.github.premnirmal.ticker.portfolio

/**
 * The current locale's decimal separator character, replacing the Android-only
 * `android.icu.text.DecimalFormatSymbols`. Backed by `DecimalFormatSymbols` on Android and
 * `NSNumberFormatter` on iOS, so the shared [DecimalFormatter] input cleanup can live in
 * `:ui-shared` `commonMain`.
 */
expect fun localeDecimalSeparator(): Char
