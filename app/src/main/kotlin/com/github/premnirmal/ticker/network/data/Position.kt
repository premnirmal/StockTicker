package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize

@Parcelize
data class Position(
  var symbol: String = "",
  var holdings: MutableList<Holding> = ArrayList()
) : Parcelable {

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

@Parcelize
data class Holding(
    val symbol: String,
    val shares: Float = 0.0f,
    val price: Float = 0.0f,
    var id: Long? = null
) : Parcelable {

  fun totalValue(): Float = shares * price
}