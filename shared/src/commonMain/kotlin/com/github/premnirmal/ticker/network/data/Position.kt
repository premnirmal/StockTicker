package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.shared.CommonParcelable
import com.github.premnirmal.shared.CommonParcelize
import kotlinx.serialization.Serializable

@CommonParcelize
@Serializable
data class Position(
    var symbol: String = "",
    var holdings: MutableList<Holding> = ArrayList()
) : CommonParcelable {

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

@CommonParcelize
data class HoldingSum(
    val totalShares: Float,
    val totalPaidPrice: Float,
    val averagePrice: Float,
) : CommonParcelable

@CommonParcelize
@Serializable
data class Holding(
    val symbol: String,
    val shares: Float = 0.0f,
    val price: Float = 0.0f,
    var id: Long? = null
) : CommonParcelable {

    fun totalValue(): Float = shares * price
}
