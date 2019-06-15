package com.github.premnirmal.ticker.network.data

data class Position(
  var symbol: String = "",
  var holdings: MutableList<Holding> = ArrayList()
) {

  fun add(holding: Holding) {
    holdings.add(holding)
  }

  fun remove(holding: Holding) {
    holdings.remove(holding)
  }

  fun averagePrice(): Float =
    holdings.sumByDouble {
      it.totalValue()
          .toDouble()
    }.div(totalShares()).toFloat()

  fun totalShares(): Float = holdings.sumByDouble { it.shares.toDouble() }.toFloat()

  fun totalPaidPrice(): Float = holdings.sumByDouble {
    it.totalValue()
        .toDouble()
  }.toFloat()
}

data class Holding(
  var shares: Float = 0.0f,
  var price: Float = 0.0f
) {

  fun totalValue(): Float = shares * price
}