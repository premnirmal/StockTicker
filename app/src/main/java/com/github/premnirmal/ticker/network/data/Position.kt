package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import kotlinx.serialization.Serializable

@Parcelize
@Serializable
data class Position(
    var symbol: String = "",
    var holdings: MutableList<Holding> = ArrayList()
) : Parcelable {

    fun add(holding: Holding) {
        holdings.add(holding)
    }

    fun remove(holding: Holding): Boolean {
        return holdings.remove(holding)
    }

    fun averagePrice(): Float {
        return holdings.averagePrice()
    }

    fun totalShares(): Float = holdings.totalShares()

    fun totalPaidPrice(): Float = holdings.totalPaidPrice()
}

fun List<Holding>.totalShares(): Float = this.sumOf { it.shares.toDouble() }.toFloat()
fun List<Holding>.totalPaidPrice(): Float = this.sumOf { it.totalValue().toDouble() }.toFloat()
fun List<Holding>.averagePrice(): Float = if (this.totalShares() == 0f) 0f else this.totalPaidPrice() / this.totalShares()

fun List<Holding>.holdingsSum(): HoldingSum {
    val totalShares = this.totalShares()
    val totalPaidPrice = this.totalPaidPrice()
    val averagePrice = this.averagePrice()
    return HoldingSum(totalShares, totalPaidPrice, averagePrice)
}

@Parcelize
data class HoldingSum(
    val totalShares: Float,
    val totalPaidPrice: Float,
    val averagePrice: Float,
) : Parcelable

@Parcelize
@Serializable
data class Holding(
    val symbol: String,
    val shares: Float = 0.0f,
    val price: Float = 0.0f,
    var id: Long? = null
) : Parcelable {

    fun totalValue(): Float = shares * price
}
