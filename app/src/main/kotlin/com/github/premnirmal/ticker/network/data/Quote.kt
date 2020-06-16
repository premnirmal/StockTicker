package com.github.premnirmal.ticker.network.data

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.github.premnirmal.ticker.AppPreferences
import java.text.Format

/**
 * Created by premnirmal on 3/30/17.
 */
data class Quote(var symbol: String = "") : Parcelable, Comparable<Quote> {

  companion object {

    @JvmField
    val CREATOR = object: Creator<Quote> {
      override fun createFromParcel(parcel: Parcel): Quote {
        return Quote(parcel)
      }

      override fun newArray(size: Int): Array<Quote?> {
        return arrayOfNulls(size)
      }
    }
  }

  var name: String = ""
  var lastTradePrice: Float = 0.toFloat()
  var changeInPercent: Float = 0.toFloat()
  var change: Float = 0.toFloat()
  var stockExchange: String = ""
  var currency: String = ""
  var description: String = ""

  var position: Position? = null

  fun hasPositions(): Boolean = position?.holdings?.isNotEmpty() ?: false

  fun changeString(): String = selectedFormat.format(change)

  fun changeStringWithSign(): String {
    val changeString = selectedFormat.format(change)
    if (change >= 0) {
      return "+$changeString"
    }
    return changeString
  }

  fun changePercentString(): String = "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"

  fun changePercentStringWithSign(): String {
    val changeString = "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"
    if (changeInPercent >= 0) {
      return "+$changeString"
    }
    return changeString
  }

  private fun positionPrice(): Float = position?.let { it ->
    it.averagePrice()
  } ?: 0f

  private fun totalPositionShares(): Float = position?.let { it ->
    it.totalShares()
  } ?: 0f

  private fun totalPositionPrice(): Float = position?.let { it ->
    it.totalPaidPrice()
  } ?: 0f

  fun priceString(): String = selectedFormat.format(lastTradePrice)

  fun averagePositionPrice(): String = selectedFormat.format(positionPrice())

  fun numSharesString(): String = selectedFormat.format(totalPositionShares())

  fun totalSpentString(): String = selectedFormat.format(totalPositionPrice())

  fun holdings(): Float = lastTradePrice * totalPositionShares()

  fun holdingsString(): String = selectedFormat.format(holdings())

  fun gainLoss(): Float = holdings() - totalPositionShares() * positionPrice()

  fun gainLossString(): String {
    val gainLoss = gainLoss()
    val gainLossString = selectedFormat.format(gainLoss)
    if (gainLoss >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  private fun gainLossPercent(): Float = (gainLoss() / totalPositionPrice()) * 100f

  fun gainLossPercentString(): String {
    val gainLossPercent = gainLossPercent()
    val gainLossString = "${AppPreferences.DECIMAL_FORMAT_2DP.format(gainLossPercent)}%"
    if (gainLossPercent >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  fun dayChange(): Float = totalPositionShares() * change

  fun dayChangeString(): String {
    val dayChange = dayChange()
    val dayChangeString = selectedFormat.format(dayChange)
    if (dayChange > 0) {
      return "+$dayChangeString"
    }
    return dayChangeString
  }

  fun newsQuery(): String {
    return "$symbol $name"
  }

  private val selectedFormat: Format
    get() = if (AppPreferences.INSTANCE.roundToTwoDecimalPlaces()) {
      AppPreferences.DECIMAL_FORMAT_2DP
    } else {
      AppPreferences.DECIMAL_FORMAT
    }

  override operator fun compareTo(other: Quote): Int =
    other.changeInPercent.compareTo(changeInPercent)

  constructor(parcel: Parcel) : this(parcel.readString()!!) {
    name = parcel.readString()!!
    lastTradePrice = parcel.readFloat()
    changeInPercent = parcel.readFloat()
    change = parcel.readFloat()
    stockExchange = parcel.readString()!!
    currency = parcel.readString()!!
    description = parcel.readString()!!
    position = parcel.readParcelable(Position::class.java.classLoader)
  }

  override fun writeToParcel(
    parcel: Parcel,
    flags: Int
  ) {
    parcel.writeString(symbol)
    parcel.writeString(name)
    parcel.writeFloat(lastTradePrice)
    parcel.writeFloat(changeInPercent)
    parcel.writeFloat(change)
    parcel.writeString(stockExchange)
    parcel.writeString(currency)
    parcel.writeString(description)
    parcel.writeParcelable(position, flags)
  }

  override fun describeContents(): Int {
    return 0
  }
}