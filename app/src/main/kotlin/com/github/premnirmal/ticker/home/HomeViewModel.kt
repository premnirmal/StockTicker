package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import javax.inject.Inject

class HomeViewModel : ViewModel() {

  @Inject lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

  val hasHoldings: Boolean
    get() = stocksProvider.hasPositions()

  fun getTotalHoldings(): Pair<String, Int> {
    var totalHolding = 0.0f
    var totalQuotesWithPosition = 0

    if (stocksProvider.getTickers().isNotEmpty()) {
      val portfolio: List<Quote> = stocksProvider.getPortfolio()

      for (quote in portfolio) {
        if (quote.hasPositions()) {
          totalQuotesWithPosition++
          totalHolding += quote.holdings()
        }
      }
    }
    return Pair("%s".format(Quote.selectedFormat.format(totalHolding)), totalQuotesWithPosition)
  }

  fun getTotalGainLoss(): Pair<String, String> {
    var totalGainStr = ""
    var totalLossStr = ""

    if (stocksProvider.getTickers().isNotEmpty()) {
      var totalGain = 0.0f
      var totalLoss = 0.0f
      val portfolio: List<Quote> = stocksProvider.getPortfolio()

      for (quote in portfolio) {
        if (quote.hasPositions()) {
          val gainLoss = quote.gainLoss()
          if (gainLoss > 0.0f) {
            totalGain += gainLoss
          } else {
            totalLoss += gainLoss
          }
        }
      }

      totalGainStr = if (totalGain != 0.0f) {
        "+" + Quote.selectedFormat.format(totalGain)
      } else {
        ""
      }

      totalLossStr = if (totalLoss != 0.0f) {
        Quote.selectedFormat.format(totalLoss)
      } else {
        ""
      }
    }
    return Pair(totalGainStr, totalLossStr)
  }

  fun fetch() = liveData {
    val fetch = stocksProvider.fetch()
    emit(fetch.wasSuccessful)
  }

  fun lastFetched(): String {
    return stocksProvider.lastFetched()
  }

  fun nextFetch(): String {
    return stocksProvider.nextFetch()
  }
}
