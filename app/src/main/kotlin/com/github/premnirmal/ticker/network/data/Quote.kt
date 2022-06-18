package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import com.github.premnirmal.ticker.AppPreferences
import kotlinx.android.parcel.Parcelize

/**
 * Created by premnirmal on 3/30/17.
 */
@Parcelize
data class Quote constructor(
  var symbol: String = "",
  var name: String = "",
  var lastTradePrice: Float = 0.toFloat(),
  var changeInPercent: Float = 0.toFloat(),
  var change: Float = 0.toFloat()
) : Parcelable, Comparable<Quote> {

  var isPostMarket: Boolean = false
  var stockExchange: String = ""
  var currencyCode: String = ""
  var annualDividendRate: Float = 0.toFloat()
  var annualDividendYield: Float = 0.toFloat()

  var position: Position? = null
  var properties: Properties? = null

  var region: String = ""
  var quoteType: String = ""
  var longName: String? = null
  var gmtOffSetMilliseconds: Long = 0
  var dayHigh: Float? = null
  var dayLow: Float? = null
  var previousClose: Float = 0.0f
  var open: Float? = null
  var regularMarketVolume: Long? = null
  var trailingPE: Float? = 0.0f
  var marketState: String = ""
  var tradeable: Boolean = false
  var triggerable: Boolean = false
  var fiftyTwoWeekLowChange: Float? = 0.0f
  var fiftyTwoWeekLowChangePercent: Float? = 0.0f
  var fiftyTwoWeekHighChange: Float? = 0.0f
  var fiftyTwoWeekHighChangePercent: Float? = 0.0f
  var fiftyTwoWeekLow: Float? = 0.0f
  var fiftyTwoWeekHigh: Float? = 0.0f
  var dividendDate: Long? = null
  var earningsTimestamp: Long? = null
  var fiftyDayAverage: Float? = 0.0f
  var fiftyDayAverageChange: Float? = 0.0f
  var fiftyDayAverageChangePercent: Float? = 0.0f
  var twoHundredDayAverage: Float? = 0.0f
  var twoHundredDayAverageChange: Float? = 0.0f
  var twoHundredDayAverageChangePercent: Float? = 0.0f
  var marketCap: Long? = null

  val priceFormat: PriceFormat
    get() = currencyCodes[currencyCode] ?: PriceFormat(currencyCode, currencyCode)

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

  fun averagePositionPrice(): String =
    AppPreferences.SELECTED_DECIMAL_FORMAT.format(positionPrice())

  fun numSharesString(): String =
    AppPreferences.SELECTED_DECIMAL_FORMAT.format(totalPositionShares())

  fun totalSpentString(): String =
    AppPreferences.SELECTED_DECIMAL_FORMAT.format(totalPositionPrice())

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
  
  fun copyValues(data: Quote) {
    this.name = data.name
    this.lastTradePrice = data.lastTradePrice
    this.changeInPercent = data.changeInPercent
    this.change = data.change
    this.stockExchange = data.stockExchange
    this.currencyCode = data.currencyCode
    this.annualDividendRate = data.annualDividendRate
    this.annualDividendYield = data.annualDividendYield
    this.region = data.region
    this.quoteType = data.quoteType
    this.longName = data.longName
    this.gmtOffSetMilliseconds = data.gmtOffSetMilliseconds
    this.dayHigh = data.dayHigh
    this.dayLow = data.dayLow
    this.previousClose = data.previousClose
    this.open = data.open
    this.regularMarketVolume = data.regularMarketVolume
    this.trailingPE = data.trailingPE
    this.marketState = data.marketState
    this.tradeable = data.tradeable
    this.fiftyTwoWeekLowChange = data.fiftyTwoWeekLowChange
    this.fiftyTwoWeekLowChangePercent = data.fiftyTwoWeekLowChangePercent
    this.fiftyTwoWeekHighChange = data.fiftyTwoWeekHighChange
    this.fiftyTwoWeekHighChangePercent = data.fiftyTwoWeekHighChangePercent
    this.fiftyTwoWeekLow = data.fiftyTwoWeekLow
    this.fiftyTwoWeekHigh = data.fiftyTwoWeekHigh
    this.dividendDate = data.dividendDate
    this.earningsTimestamp = data.earningsTimestamp
    this.fiftyDayAverage = data.fiftyDayAverage
    this.fiftyDayAverageChange = data.fiftyDayAverageChange
    this.fiftyDayAverageChangePercent = data.fiftyDayAverageChangePercent
    this.twoHundredDayAverage = data.twoHundredDayAverage
    this.twoHundredDayAverageChange = data.twoHundredDayAverageChange
    this.twoHundredDayAverageChangePercent = data.twoHundredDayAverageChangePercent
    this.marketCap = data.marketCap
  }

  companion object {

    private val currencyCodes: Map<String, PriceFormat> by lazy {
      mapOf(
        "USD" to PriceFormat("USD", "$"),
        "CAD" to PriceFormat("CAD", "$"),
        "EUR" to PriceFormat("EUR", "€"),
        "AED" to PriceFormat("AED", "د.إ.‏"),
        "AFN" to PriceFormat("AFN", "؋"),
        "ALL" to PriceFormat("ALL", "Lek"),
        "AMD" to PriceFormat("AMD", "դր."),
        "ARS" to PriceFormat("ARS", "$"),
        "AUD" to PriceFormat("AUD", "$"),
        "AZN" to PriceFormat("AZN", "ман."),
        "BAM" to PriceFormat("BAM", "KM"),
        "BDT" to PriceFormat("BDT", "৳"),
        "BGN" to PriceFormat("BGN", "лв."),
        "BHD" to PriceFormat("BHD", "د.ب.‏"),
        "BIF" to PriceFormat("BIF", "FBu"),
        "BND" to PriceFormat("BND", "$"),
        "BOB" to PriceFormat("BOB", "Bs"),
        "BRL" to PriceFormat("BRL", "R$"),
        "BWP" to PriceFormat("BWP", "P"),
        "BYN" to PriceFormat("BYN", "руб."),
        "BZD" to PriceFormat("BZD", "$"),
        "CDF" to PriceFormat("CDF", "FrCD"),
        "CHF" to PriceFormat("CHF", "CHF"),
        "CLP" to PriceFormat("CLP", "$"),
        "CNY" to PriceFormat("CNY", "CN¥"),
        "COP" to PriceFormat("COP", "$"),
        "CRC" to PriceFormat("CRC", "₡"),
        "CVE" to PriceFormat("CVE", "CV$"),
        "CZK" to PriceFormat("CZK", "Kč"),
        "DJF" to PriceFormat("DJF", "Fdj"),
        "DKK" to PriceFormat("DKK", "kr"),
        "DOP" to PriceFormat("DOP", "RD$"),
        "DZD" to PriceFormat("DZD", "د.ج.‏"),
        "EEK" to PriceFormat("EEK", "kr"),
        "EGP" to PriceFormat("EGP", "ج.م.‏"),
        "ERN" to PriceFormat("ERN", "Nfk"),
        "ETB" to PriceFormat("ETB", "Br"),
        "GBP" to PriceFormat("GBP", "£"),
        "GBp" to PriceFormat("GBp", "p", prefix = false),
        "GEL" to PriceFormat("GEL", "GEL"),
        "GHS" to PriceFormat("GHS", "GH₵"),
        "GNF" to PriceFormat("GNF", "FG"),
        "GTQ" to PriceFormat("GTQ", "Q"),
        "HKD" to PriceFormat("HKD", "$"),
        "HNL" to PriceFormat("HNL", "L"),
        "HRK" to PriceFormat("HRK", "kn"),
        "HUF" to PriceFormat("HUF", "Ft"),
        "IDR" to PriceFormat("IDR", "Rp"),
        "ILS" to PriceFormat("ILS", "₪"),
        "INR" to PriceFormat("INR", "₹"),
        "IQD" to PriceFormat("IQD", "د.ع.‏"),
        "IRR" to PriceFormat("IRR", "﷼"),
        "ISK" to PriceFormat("ISK", "kr"),
        "JMD" to PriceFormat("JMD", "$"),
        "JOD" to PriceFormat("JOD", "د.أ.‏"),
        "JPY" to PriceFormat("JPY", "￥"),
        "KES" to PriceFormat("KES", "Ksh"),
        "KHR" to PriceFormat("KHR", "៛"),
        "KMF" to PriceFormat("KMF", "FC"),
        "KRW" to PriceFormat("KRW", "₩"),
        "KWD" to PriceFormat("KWD", "د.ك.‏"),
        "KZT" to PriceFormat("KZT", "тңг."),
        "LBP" to PriceFormat("LBP", "ل.ل.‏"),
        "LKR" to PriceFormat("LKR", "SLRe"),
        "LTL" to PriceFormat("LTL", "Lt"),
        "LVL" to PriceFormat("LVL", "Ls"),
        "LYD" to PriceFormat("LYD", "د.ل.‏"),
        "MAD" to PriceFormat("MAD", "د.م.‏"),
        "MDL" to PriceFormat("MDL", "MDL"),
        "MGA" to PriceFormat("MGA", "MGA"),
        "MKD" to PriceFormat("MKD", "MKD"),
        "MMK" to PriceFormat("MMK", "K"),
        "MOP" to PriceFormat("MOP", "MOP$"),
        "MUR" to PriceFormat("MUR", "MURs"),
        "MXN" to PriceFormat("MXN", "$"),
        "MYR" to PriceFormat("MYR", "RM"),
        "MZN" to PriceFormat("MZN", "MTn"),
        "NAD" to PriceFormat("NAD", "N$"),
        "NGN" to PriceFormat("NGN", "₦"),
        "NIO" to PriceFormat("NIO", "C$"),
        "NOK" to PriceFormat("NOK", "kr"),
        "NPR" to PriceFormat("NPR", "नेरू"),
        "NZD" to PriceFormat("NZD", "$"),
        "OMR" to PriceFormat("OMR", "ر.ع.‏"),
        "PAB" to PriceFormat("PAB", "B/."),
        "PEN" to PriceFormat("PEN", "S/."),
        "PHP" to PriceFormat("PHP", "₱"),
        "PKR" to PriceFormat("PKR", "₨"),
        "PLN" to PriceFormat("PLN", "zł"),
        "PYG" to PriceFormat("PYG", "₲"),
        "QAR" to PriceFormat("QAR", "ر.ق.‏"),
        "RON" to PriceFormat("RON", "RON"),
        "RSD" to PriceFormat("RSD", "дин."),
        "RUB" to PriceFormat("RUB", "₽."),
        "RWF" to PriceFormat("RWF", "FR"),
        "SAR" to PriceFormat("SAR", "ر.س.‏"),
        "SDG" to PriceFormat("SDG", "SDG"),
        "SEK" to PriceFormat("SEK", "kr"),
        "SGD" to PriceFormat("SGD", "$"),
        "SOS" to PriceFormat("SOS", "Ssh"),
        "SYP" to PriceFormat("SYP", "ل.س.‏"),
        "THB" to PriceFormat("THB", "฿"),
        "TND" to PriceFormat("TND", "د.ت.‏"),
        "TOP" to PriceFormat("TOP", "T$"),
        "TRY" to PriceFormat("TRY", "TL"),
        "TTD" to PriceFormat("TTD", "$"),
        "TWD" to PriceFormat("TWD", "NT$"),
        "TZS" to PriceFormat("TZS", "TSh"),
        "UAH" to PriceFormat("UAH", "₴"),
        "UGX" to PriceFormat("UGX", "USh"),
        "UYU" to PriceFormat("UYU", "$"),
        "UZS" to PriceFormat("UZS", "UZS"),
        "VEF" to PriceFormat("VEF", "Bs.F."),
        "VND" to PriceFormat("VND", "₫"),
        "XAF" to PriceFormat("XAF", "FCFA"),
        "XOF" to PriceFormat("XOF", "CFA"),
        "YER" to PriceFormat("YER", "ر.ي.‏"),
        "ZAR" to PriceFormat("ZAR", "R"),
        "ZMK" to PriceFormat("ZMK", "ZK"),
        "ZWL" to PriceFormat("ZWL", "ZWL$")
      )
    }
  }
}