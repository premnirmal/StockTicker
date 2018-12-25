package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.AppPreferences

/**
 * Created by premnirmal on 3/30/17.
 */
data class Quote(var symbol: String = "") : Comparable<Quote> {

  companion object {
    fun fromQuoteNet(quoteNet: QuoteNet): Quote {
      val quote = Quote(quoteNet.symbol ?: "")
      quote.name = quoteNet.name ?: ""
      quote.lastTradePrice = quoteNet.lastTradePrice
      quote.changeInPercent = quoteNet.changePercent
      quote.change = quoteNet.change
      quote.stockExchange = quoteNet.exchange ?: ""
      quote.currency = quoteNet.currency ?: "US"
      quote.description = quoteNet.description ?: ""
      return quote
    }
  }

  var name: String = ""
  var lastTradePrice: Float = 0.toFloat()
  var changeInPercent: Float = 0.toFloat()
  var change: Float = 0.toFloat()
  var stockExchange: String = ""
  var currency: String = ""
  var description: String = ""

  // Position fields

  @Deprecated("remove after migration") var isPosition: Boolean = false
  @Deprecated("remove after migration") var positionPrice: Float = 0.toFloat()
  @Deprecated("remove after migration") var positionShares: Float = 0.toFloat()
  var position: Position? = null

  fun hasPositions(): Boolean = position?.holdings?.isNotEmpty() ?: false

  fun changeString(): String = AppPreferences.DECIMAL_FORMAT.format(change)

  fun changeStringWithSign(): String {
    val changeString = AppPreferences.DECIMAL_FORMAT.format(change)
    if (change >= 0) {
      return "+$changeString"
    }
    return changeString
  }

  fun changePercentString(): String = "${AppPreferences.DECIMAL_FORMAT.format(changeInPercent)}%"

  fun changePercentStringWithSign(): String {
    val changeString = "${AppPreferences.DECIMAL_FORMAT.format(changeInPercent)}%"
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

  fun priceString(): String = AppPreferences.DECIMAL_FORMAT.format(lastTradePrice)

  fun averagePositionPrice(): String = AppPreferences.DECIMAL_FORMAT.format(positionPrice())

  fun numSharesString(): String = AppPreferences.DECIMAL_FORMAT.format(totalPositionShares())

  fun totalSpentString(): String = AppPreferences.DECIMAL_FORMAT.format(totalPositionPrice())

  fun holdings(): Float = lastTradePrice * totalPositionShares()

  fun holdingsString(): String = AppPreferences.DECIMAL_FORMAT.format(holdings())

  fun gainLoss(): Float = holdings() - totalPositionShares() * positionPrice()

  fun gainLossString(): String {
    val gainLoss = gainLoss()
    val gainLossString = AppPreferences.DECIMAL_FORMAT.format(gainLoss)
    if (gainLoss >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  private fun gainLossPercent(): Float = (gainLoss() / holdings()) * 100f

  fun gainLossPercentString(): String {
    val gainLossPercent = gainLossPercent()
    val gainLossString = "${AppPreferences.DECIMAL_FORMAT.format(gainLossPercent)}%"
    if (gainLossPercent >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  fun dayChange(): Float = totalPositionShares() * change

  fun dayChangeString(): String {
    val dayChange = dayChange()
    val dayChangeString = AppPreferences.DECIMAL_FORMAT.format(dayChange)
    if (dayChange > 0) {
      return "+$dayChangeString"
    }
    return dayChangeString
  }

  fun newsQuery(): String {
    if (name.isEmpty()) return "$symbol stock"
    val split = name.replace("[^\\w\\s]", "").split(" ")
    return if (split.size <= 3) {
      name
    } else {
      split.subList(0, 2).joinToString(separator = " ") + " stock"
    }
  }

  override operator fun compareTo(other: Quote): Int =
    other.changeInPercent.compareTo(changeInPercent)
}