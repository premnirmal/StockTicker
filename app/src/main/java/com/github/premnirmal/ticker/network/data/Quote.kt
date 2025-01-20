package com.github.premnirmal.ticker.network.data

import android.os.Parcelable
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.tickerwidget.ui.theme.ColourPalette
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
  var currencyCode: String = "USD"
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
    get() = currencyCodes[currencyCode]?.let {
      PriceFormat(currencyCode = currencyCode, symbol = it, prefix = prefixCurrencies[currencyCode] ?: true)
    } ?: PriceFormat(currencyCode, currencyCode)

  fun showAlertAbove(): Boolean =
    this.properties != null && this.properties!!.alertAbove > 0.0f && this.properties!!.alertAbove < this.lastTradePrice

  fun showAlertBelow(): Boolean =
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

  val isUp: Boolean
    get() = change > 0f

  val isDown: Boolean
    get() = change < 0f

  val changeColour: Color
    @Composable get() = if (isUp) ColourPalette.ChangePositive else ColourPalette.ChangeNegative

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

  private fun gainLossPercent(): Float {
    if (totalPositionPrice() == 0f) return 0f
    return (gainLoss() / totalPositionPrice()) * 100f
  }

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
    return "${if (symbol.contains(".")) symbol.substring(0, symbol.indexOf('.')) else symbol} ${name.split(" ").toMutableList().apply { removeAll(arrayOf("Inc.", "Corporation", "PLC", "ORD")) }.take(3).joinToString(" ")} stock"
  }

  val isMarketOpen: Boolean
    get() = "REGULAR" == marketState.uppercase()

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

    private val currencyCodes: Map<String, String> by lazy {
      mapOf(
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
        "GBp" to "p",
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

    private val prefixCurrencies: Map<String, Boolean> by lazy {
      mapOf("GBp" to false)
    }
  }
}