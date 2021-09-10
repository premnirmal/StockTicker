package com.github.premnirmal.ticker.network.data

import android.os.Parcel
import android.os.Parcelable
import android.os.Parcelable.Creator
import com.github.premnirmal.ticker.AppPreferences

/**
 * Created by premnirmal on 3/30/17.
 */
data class Quote(var symbol: String = "") : Parcelable, Comparable<Quote> {

  var name: String = ""
  var lastTradePrice: Float = 0.toFloat()
  var changeInPercent: Float = 0.toFloat()
  var change: Float = 0.toFloat()
  var isPostMarket: Boolean = false
  var stockExchange: String = ""
  var currencyCode: String = ""
  var annualDividendRate: Float = 0.toFloat()
  var annualDividendYield: Float = 0.toFloat()

  var position: Position? = null
  var properties: Properties? = null

  val currencySymbol: String
    get() = currencyCodes[currencyCode].orEmpty()

  fun hasAlertAbove(): Boolean =
    this.properties != null && this.properties!!.alertAbove > 0.0f && this.properties!!.alertAbove < this.lastTradePrice

  fun hasAlertBelow(): Boolean =
    this.properties != null && this.properties!!.alertBelow > 0.0f && this.properties!!.alertBelow > this.lastTradePrice

  fun getAlertAbove(): Float = this.properties?.alertAbove ?: 0.0f

  fun getAlertBelow(): Float = this.properties?.alertBelow ?: 0.0f

  fun hasPositions(): Boolean = position?.holdings?.isNotEmpty() ?: false

  fun changeString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(change)

  fun changeStringWithSign(): String {
    val changeString = AppPreferences.SELECTED_DECIMAL_FORMAT.format(change)
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
      AppPreferences.DECIMAL_FORMAT_2DP.format(annualDividendRate)
    }
  }

  private fun positionPrice(): Float = position?.averagePrice() ?: 0f

  private fun totalPositionShares(): Float = position?.totalShares() ?: 0f

  private fun totalPositionPrice(): Float = position?.totalPaidPrice() ?: 0f

  fun priceString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(lastTradePrice)

  fun averagePositionPrice(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(positionPrice())

  fun numSharesString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(totalPositionShares())

  fun totalSpentString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(totalPositionPrice())

  fun holdings(): Float = lastTradePrice * totalPositionShares()

  fun holdingsString(): String = AppPreferences.SELECTED_DECIMAL_FORMAT.format(holdings())

  fun gainLoss(): Float = holdings() - totalPositionShares() * positionPrice()

  fun gainLossString(): String {
    val gainLoss = gainLoss()
    val gainLossString = AppPreferences.SELECTED_DECIMAL_FORMAT.format(gainLoss)
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
    val dayChangeString = AppPreferences.SELECTED_DECIMAL_FORMAT.format(dayChange)
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
    currencyCode = parcel.readString()!!
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
    parcel.writeString(currencyCode)
    parcel.writeFloat(annualDividendRate)
    parcel.writeFloat(annualDividendYield)
    parcel.writeParcelable(position, flags)
    parcel.writeParcelable(properties, flags)
  }

  override fun describeContents(): Int {
    return 0
  }

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

    private val currencyCodes = mapOf(
        "USD" to "$",
        "CAD" to "$",
        "EUR" to "€",
        "AED" to "د.إ.‏",
        "AFN" to "؋",
        "ALL" to "Lek",
        "AMD" to "դր.",
        "ARS" to "$",
        "AUD" to "$",
        "AZN" to "ман.",
        "BAM" to "KM",
        "BDT" to "৳",
        "BGN" to "лв.",
        "BHD" to "د.ب.‏",
        "BIF" to "FBu",
        "BND" to "$",
        "BOB" to "Bs",
        "BRL" to "R$",
        "BWP" to "P",
        "BYN" to "руб.",
        "BZD" to "$",
        "CDF" to "FrCD",
        "CHF" to "CHF",
        "CLP" to "$",
        "CNY" to "CN¥",
        "COP" to "$",
        "CRC" to "₡",
        "CVE" to "CV$",
        "CZK" to "Kč",
        "DJF" to "Fdj",
        "DKK" to "kr",
        "DOP" to "RD$",
        "DZD" to "د.ج.‏",
        "EEK" to "kr",
        "EGP" to "ج.م.‏",
        "ERN" to "Nfk",
        "ETB" to "Br",
        "GBP" to "£",
        "GBp" to "GBp",
        "GEL" to "GEL",
        "GHS" to "GH₵",
        "GNF" to "FG",
        "GTQ" to "Q",
        "HKD" to "$",
        "HNL" to "L",
        "HRK" to "kn",
        "HUF" to "Ft",
        "IDR" to "Rp",
        "ILS" to "₪",
        "INR" to "₹",
        "IQD" to "د.ع.‏",
        "IRR" to "﷼",
        "ISK" to "kr",
        "JMD" to "$",
        "JOD" to "د.أ.‏",
        "JPY" to "￥",
        "KES" to "Ksh",
        "KHR" to "៛",
        "KMF" to "FC",
        "KRW" to "₩",
        "KWD" to "د.ك.‏",
        "KZT" to "тңг.",
        "LBP" to "ل.ل.‏",
        "LKR" to "SLRe",
        "LTL" to "Lt",
        "LVL" to "Ls",
        "LYD" to "د.ل.‏",
        "MAD" to "د.م.‏",
        "MDL" to "MDL",
        "MGA" to "MGA",
        "MKD" to "MKD",
        "MMK" to "K",
        "MOP" to "MOP$",
        "MUR" to "MURs",
        "MXN" to "$",
        "MYR" to "RM",
        "MZN" to "MTn",
        "NAD" to "N$",
        "NGN" to "₦",
        "NIO" to "C$",
        "NOK" to "kr",
        "NPR" to "नेरू",
        "NZD" to "$",
        "OMR" to "ر.ع.‏",
        "PAB" to "B/.",
        "PEN" to "S/.",
        "PHP" to "₱",
        "PKR" to "₨",
        "PLN" to "zł",
        "PYG" to "₲",
        "QAR" to "ر.ق.‏",
        "RON" to "RON",
        "RSD" to "дин.",
        "RUB" to "₽.",
        "RWF" to "FR",
        "SAR" to "ر.س.‏",
        "SDG" to "SDG",
        "SEK" to "kr",
        "SGD" to "$",
        "SOS" to "Ssh",
        "SYP" to "ل.س.‏",
        "THB" to "฿",
        "TND" to "د.ت.‏",
        "TOP" to "T$",
        "TRY" to "TL",
        "TTD" to "$",
        "TWD" to "NT$",
        "TZS" to "TSh",
        "UAH" to "₴",
        "UGX" to "USh",
        "UYU" to "$",
        "UZS" to "UZS",
        "VEF" to "Bs.F.",
        "VND" to "₫",
        "XAF" to "FCFA",
        "XOF" to "CFA",
        "YER" to "ر.ي.‏",
        "ZAR" to "R",
        "ZMK" to "ZK",
        "ZWL" to "ZWL$"
    )
  }
}