package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.components.AppNumberFormat
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Verifies [PriceFormat] — migrated into commonMain together with the multiplatform
 * [com.github.premnirmal.ticker.components.DecimalFormatter] / [AppNumberFormat] abstraction —
 * formats prices on every Kotlin Multiplatform target.
 */
class PriceFormatTest {

    @AfterTest
    fun tearDown() {
        // Restore the default so other tests are unaffected by the shared flag.
        AppNumberFormat.roundToTwoDecimalPlaces = true
    }

    @Test
    fun prefixesSymbolWhenPrefixTrue() {
        AppNumberFormat.roundToTwoDecimalPlaces = true
        val usd = PriceFormat(currencyCode = "USD", symbol = "$", prefix = true)
        assertTrue(usd.format(10f).startsWith("$"))
    }

    @Test
    fun suffixesSymbolWhenPrefixFalse() {
        AppNumberFormat.roundToTwoDecimalPlaces = true
        val gbp = PriceFormat(currencyCode = "GBp", symbol = "p", prefix = false)
        assertTrue(gbp.format(10f).endsWith("p"))
    }

    @Test
    fun roundingFollowsSelectedFormat() {
        val pf = PriceFormat(currencyCode = "USD", symbol = "$")

        AppNumberFormat.roundToTwoDecimalPlaces = true
        val twoDp = pf.format(1.23456f)

        AppNumberFormat.roundToTwoDecimalPlaces = false
        val moreDp = pf.format(1.23456f)

        // 2dp rounds to "1.23"; the default format keeps up to four fraction digits ("1.2346"),
        // so the two outputs must differ regardless of the platform's locale separators.
        assertNotEquals(twoDp, moreDp)
    }
}
