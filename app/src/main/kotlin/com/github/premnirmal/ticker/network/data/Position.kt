package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.AppPreferences

data class Position(var symbol: String = "", var holdings: MutableList<Holding> = ArrayList()) {

  fun add(holding: Holding) {
    holdings.add(holding)
  }

  fun remove(holding: Holding) {
    holdings.remove(holding)
  }
}

data class Holding(var shares: Float = 0.0f, var price: Float = 0.0f) {

  fun totalValue(): Float = shares * price

  fun holdingsText(): String {
    val shares = AppPreferences.DECIMAL_FORMAT.format(shares)
    val price = AppPreferences.DECIMAL_FORMAT.format(price)
    val total = AppPreferences.DECIMAL_FORMAT.format(totalValue())
    return "$shares x $price = $total"
  }
}