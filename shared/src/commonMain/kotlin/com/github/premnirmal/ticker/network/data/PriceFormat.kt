package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.components.AppNumberFormat

class PriceFormat(
    val currencyCode: String,
    val symbol: String,
    val prefix: Boolean = true
) {
    fun format(price: Float): String {
        val priceString = AppNumberFormat.selected.format(price)
        return if (prefix) {
            "$symbol$priceString"
        } else {
            "$priceString$symbol"
        }
    }
}
