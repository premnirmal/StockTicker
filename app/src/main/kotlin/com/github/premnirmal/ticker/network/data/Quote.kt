package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.Tools
import java.io.Serializable

/**
 * Created by premnirmal on 3/30/17.
 */
data class Quote(var symbol: String = "",
    var name: String = "",
    var lastTradePrice: Float = 0.toFloat(),
    var changeInPercent: Float = 0.toFloat(),
    var change: Float = 0.toFloat(),
    var stockExchange: String = "",
    var currency: String = "") : Comparable<Quote>, Serializable {

  companion object {
    private val serialVersionUID = -4235355L
  }

  // Add Position fields
  var isPosition: Boolean = false
  var positionPrice: Float = 0.toFloat()
  var positionShares: Int = 0

  override fun equals(other: Any?): Boolean {
    if (other is Quote) {
      return symbol.equals(other.symbol, ignoreCase = true)
    } else if (other is String) {
      return other.equals(symbol, ignoreCase = true)
    }
    return false
  }

  override fun hashCode(): Int {
    return symbol.hashCode()
  }

  override fun toString(): String {
    return symbol
  }

  fun isIndex(): Boolean {
    return symbol.startsWith("^") || symbol.contains("=")
  }

  fun changeString(): String {
    return Tools.DECIMAL_FORMAT.format(change)
  }

  fun changePercentString(): String {
    return "${Tools.DECIMAL_FORMAT.format(changeInPercent)}%"
  }

  fun priceString(): String {
    return Tools.DECIMAL_FORMAT.format(lastTradePrice)
  }

  fun holdings(): Float {
    return lastTradePrice * positionShares
  }

  fun holdingsString(): String {
    return Tools.DECIMAL_FORMAT.format(holdings())
  }

  fun gainLoss(): Float {
    return holdings() - positionShares * positionPrice
  }

  fun gainLossString(): String {
    return Tools.DECIMAL_FORMAT.format(gainLoss())
  }

  fun dayChange(): Float {
    return lastTradePrice - positionPrice
  }

  fun dayChangeString(): String {
    return Tools.DECIMAL_FORMAT.format(dayChange())
  }

  fun dayChangePercent(): Float {
    return dayChange() / positionPrice
  }

  fun dayChangePercentString(): String {
    return "${Tools.DECIMAL_FORMAT.format(dayChangePercent() * 100)}%"
  }

  override operator fun compareTo(other: Quote): Int {
    return java.lang.Float.compare(other.changeInPercent, changeInPercent)
  }
}