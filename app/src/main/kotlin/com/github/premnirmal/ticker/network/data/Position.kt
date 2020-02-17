package com.github.premnirmal.ticker.network.data

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator

data class Position(
  var symbol: String = "",
  var holdings: MutableList<Holding> = ArrayList()
) : Parcelable {

  constructor(parcel: Parcel) : this(
      parcel.readString()!!) {
    val tmpArray = parcel.readParcelableArray(Holding::class.java.classLoader)
    if (tmpArray != null && tmpArray.isNotEmpty()) {
      tmpArray.forEach {
        val holding = it as Holding
        holdings.add(holding)
      }
    }
  }

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

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(symbol)
    parcel.writeParcelableArray(holdings.toTypedArray(), flags)
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object {
    @JvmField
    val CREATOR = object: Creator<Position> {
      override fun createFromParcel(parcel: Parcel): Position {
        return Position(parcel)
      }

      override fun newArray(size: Int): Array<Position?> {
        return arrayOfNulls(size)
      }
    }
  }
}

data class Holding(
    val symbol: String,
    val shares: Float = 0.0f,
    val price: Float = 0.0f,
    var id: Long? = null
) : Parcelable {

  fun totalValue(): Float = shares * price

  constructor(parcel: Parcel) : this(
      parcel.readString()!!,
      parcel.readFloat(),
      parcel.readFloat(),
      parcel.readLong())

  override fun writeToParcel(parcel: Parcel, flags: Int) {
    parcel.writeString(symbol)
    parcel.writeFloat(shares)
    parcel.writeFloat(price)
    id?.let { parcel.writeLong(it) }
  }

  override fun describeContents(): Int {
    return 0
  }

  companion object {
    @JvmField
    val CREATOR = object: Creator<Holding> {
      override fun createFromParcel(parcel: Parcel): Holding {
        return Holding(parcel)
      }

      override fun newArray(size: Int): Array<Holding?> {
        return arrayOfNulls(size)
      }
    }
  }
}