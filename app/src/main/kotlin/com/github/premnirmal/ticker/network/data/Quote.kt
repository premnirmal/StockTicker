package com.github.premnirmal.ticker.network.data

import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import java.text.Format
import javax.inject.Inject

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
    fun fromQuoteNet(quoteNet: YahooQuoteNet): Quote {
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

  @Inject internal lateinit var appPreferences: AppPreferences

  init {
    Injector.appComponent.inject(this)
  }

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
    return symbol
  }

  private val selectedFormat: Format
    get() = if (appPreferences.roundToTwoDecimalPlaces()) {
      AppPreferences.DECIMAL_FORMAT_2DP
    } else {
      AppPreferences.DECIMAL_FORMAT
    }

  override operator fun compareTo(other: Quote): Int =
    other.changeInPercent.compareTo(changeInPercent)
}