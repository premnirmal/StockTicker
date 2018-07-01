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

  @Deprecated("please use addPosition / decreasePosition")
  var positionPrice: Float = 0.toFloat()
  @Deprecated("please use addPosition / decreasePosition")
  var positionShares: Float = 0.toFloat()

  private var _totalShares : Float = 0.toFloat()
  private var _totalPositionPrice : Float = 0.toFloat()

  // Position fields
  fun isPosition() : Boolean {
    return  totalPosition() > 0
  }

  fun totalPositionPrice() : Float {
    return this._totalPositionPrice
  }

  fun totalPosition() : Float {
    return this._totalShares
  }

  fun averagePositionPrice() : Float {
    return totalPositionPrice() / totalPosition()
  }

  fun addPosition(price: Float, shares: Float){
    _totalPositionPrice += price * shares
    this._totalShares += shares
  }

  fun decreasePosition(shares: Float){
    _totalPositionPrice -= shares * averagePositionPrice()
    this._totalShares -= shares

    if(totalPosition() < 1.toFloat() ){
      _totalPositionPrice = 0.toFloat()
      _totalShares = 0.toFloat()
    }
  }

  fun isIndex(): Boolean = symbol.startsWith("^") || symbol.startsWith(".") || symbol.contains("=")

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

  fun priceString(): String = AppPreferences.DECIMAL_FORMAT.format(lastTradePrice)

  fun positionPriceString(): String = AppPreferences.DECIMAL_FORMAT.format(averagePositionPrice())

  fun numSharesString(): String = AppPreferences.DECIMAL_FORMAT.format(totalPosition())

  fun holdings(): Float = lastTradePrice * totalPosition()

  fun holdingsString(): String = AppPreferences.DECIMAL_FORMAT.format(holdings())

  fun gainLoss(): Float = holdings() - totalPositionPrice()

  fun gainLossString(): String {
    val gainLoss = gainLoss()
    val gainLossString = AppPreferences.DECIMAL_FORMAT.format(gainLoss)
    if (gainLoss >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  fun gainLossPercent(): Float = (gainLoss() / holdings()) * 100f

  fun gainLossPercentString(): String {
    val gainLossPercent = gainLossPercent()
    val gainLossString = "${AppPreferences.DECIMAL_FORMAT.format(gainLossPercent)}%"
    if (gainLossPercent >= 0) {
      return "+$gainLossString"
    }
    return gainLossString
  }

  fun dayChange(): Float = totalPosition() * change

  fun dayChangeString(): String {
    val dayChange = dayChange()
    val dayChangeString = AppPreferences.DECIMAL_FORMAT.format(dayChange)
    if (dayChange > 0) {
      return "+$dayChangeString"
    }
    return dayChangeString
  }

  fun newsQuery(): String {
    if (name.isEmpty()) return symbol + " stock"
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