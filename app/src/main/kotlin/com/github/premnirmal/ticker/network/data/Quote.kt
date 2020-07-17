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
    val CREATOR = object : Creator<Quote> {
      override fun createFromParcel(parcel: Parcel): Quote {
        return Quote(parcel)
      }

      override fun newArray(size: Int): Array<Quote?> {
        return arrayOfNulls(size)
      }
    }

    // TODO: this should be in a tools helper class.
    // Made it accessible in this class for now.
    val selectedFormat: Format
      get() = if (AppPreferences.INSTANCE.roundToTwoDecimalPlaces()) {
        AppPreferences.DECIMAL_FORMAT_2DP
      } else {
        AppPreferences.DECIMAL_FORMAT
      }
  }

  var name: String = ""
  var lastTradePrice: Float = 0.toFloat()
  var changeInPercent: Float = 0.toFloat()
  var change: Float = 0.toFloat()
  var isPostMarket: Boolean = false
  var stockExchange: String = ""
  var currency: String = ""
  var annualDividendRate: Float = 0.toFloat()
  var annualDividendYield: Float = 0.toFloat()

  var position: Position? = null
  var properties: Properties? = null

  fun hasAlertAbove(): Boolean =
    this.properties != null && this.properties!!.alertAbove > 0.0f && this.properties!!.alertAbove < this.lastTradePrice

  fun hasAlertBelow(): Boolean =
    this.properties != null && this.properties!!.alertBelow > 0.0f && this.properties!!.alertBelow > this.lastTradePrice

  fun getAlertAbove(): Float = this.properties?.alertAbove ?: 0.0f

  fun getAlertBelow(): Float = this.properties?.alertBelow ?: 0.0f

  fun hasPositions(): Boolean = position?.holdings?.isNotEmpty() ?: false

  fun changeString(): String = selectedFormat.format(change)

  fun changeStringWithSign(): String {
    val changeString = selectedFormat.format(change)
    if (change >= 0) {
      return "+$changeString"
    }
    return changeString
  }

  fun changePercentString(): String =
    "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"

  fun changePercentStringWithSign(): String {
    val changeString = "${AppPreferences.DECIMAL_FORMAT_2DP.format(changeInPercent)}%"
    if (changeInPercent >= 0) {
      return "+$changeString"
    }
    return changeString
  }
  fun dividendInfo(): String {
    return if (annualDividendRate <= 0f || annualDividendYield <= 0f) {
      "--"
    } else {
      "${AppPreferences.DECIMAL_FORMAT_2DP.format(annualDividendRate)}%"
    }
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

  override operator fun compareTo(other: Quote): Int =
    other.changeInPercent.compareTo(changeInPercent)

  constructor(parcel: Parcel) : this(parcel.readString()!!) {
    name = parcel.readString()!!
    lastTradePrice = parcel.readFloat()
    changeInPercent = parcel.readFloat()
    change = parcel.readFloat()
    isPostMarket = parcel.readInt() != 0
    stockExchange = parcel.readString()!!
    currency = parcel.readString()!!
    annualDividendRate = parcel.readFloat()
    annualDividendYield = parcel.readFloat()
    position = parcel.readParcelable(Position::class.java.classLoader)
    properties = parcel.readParcelable(
        Properties::class.java.classLoader)
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
    parcel.writeInt(if (isPostMarket) 1 else 0)
    parcel.writeString(stockExchange)
    parcel.writeString(currency)
    parcel.writeFloat(annualDividendRate)
    parcel.writeFloat(annualDividendYield)
    parcel.writeParcelable(position, flags)
    parcel.writeParcelable(properties, flags)
  }

  override fun describeContents(): Int {
    return 0
  }
}