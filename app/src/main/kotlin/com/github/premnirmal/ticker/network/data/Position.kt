package com.github.premnirmal.ticker.network.data

data class Position(var symbol: String = "", var holdings: MutableList<Holding> = ArrayList()) {

  fun add(holding: Holding) {
    holdings.add(holding)
  }

  fun remove(holding: Holding) {
    holdings.remove(holding)
  }
}

data class Holding(var shares: Float = 0.0f, var price: Float = 0.0f)