package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.AppPreferences

/**
 * Created by premnirmal on 3/30/17.
 */
data class Quote(var symbol: String = "",
    var name: String = "",
    var lastTradePrice: Float = 0.toFloat(),
    var changeInPercent: Float = 0.toFloat(),
    var change: Float = 0.toFloat(),
    var stockExchange: String = "",
    var currency: String = "",
    var description: String = "") : Comparable<Quote> {

  // Add Position fields
  var isPosition: Boolean = false
  var positionPrice: Float = 0.toFloat()
  var positionShares: Float = 0.toFloat()

  override fun equals(other: Any?): Boolean {
    if (other is Quote) {
      return symbol.equals(other.symbol, ignoreCase = true)
    } else if (other is String) {
      return other.equals(symbol, ignoreCase = true)
    }
    return false
  }

  override fun hashCode(): Int = symbol.hashCode()

  fun isIndex(): Boolean = symbol.startsWith("^") || symbol.startsWith(".") || symbol.contains("=")

  fun changeString(): String = AppPreferences.DECIMAL_FORMAT.format(change)

  fun changePercentString(): String = "${AppPreferences.DECIMAL_FORMAT.format(changeInPercent)}%"

  fun priceString(): String = AppPreferences.DECIMAL_FORMAT.format(lastTradePrice)

  fun numSharesString(): String = AppPreferences.DECIMAL_FORMAT.format(positionShares)

  private fun holdings(): Float = lastTradePrice * positionShares

  fun holdingsString(): String = AppPreferences.DECIMAL_FORMAT.format(holdings())

  fun gainLoss(): Float = holdings() - positionShares * positionPrice

  fun gainLossString(): String = AppPreferences.DECIMAL_FORMAT.format(gainLoss())

  fun gainLossPercent(): Float = (gainLoss() / holdings()) * 100f

  fun gainLossPercentString(): String = "${AppPreferences.DECIMAL_FORMAT.format(gainLossPercent())}%"

  fun dayChange(): Float = lastTradePrice - positionPrice

  fun dayChangeString(): String = AppPreferences.DECIMAL_FORMAT.format(dayChange())

  fun dayChangePercent(): Float = dayChange() / positionPrice

  fun dayChangePercentString(): String = "${AppPreferences.DECIMAL_FORMAT.format(
      dayChangePercent() * 100)}%"

  fun newsQuery(): String {
    if (name.isEmpty()) return symbol + " stock"
    val split = name.replace("[^\\w\\s]","").split(" ")
    return if (split.size <= 3) {
      name
    } else {
      split.subList(0,2).joinToString(separator = " ") + " stock"
    }
  }

  override operator fun compareTo(other: Quote): Int = java.lang.Float.compare(
      other.changeInPercent, changeInPercent)
}