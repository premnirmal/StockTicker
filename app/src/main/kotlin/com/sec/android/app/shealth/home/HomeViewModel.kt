package com.sec.android.app.shealth.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import com.sec.android.app.shealth.AppPreferences
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.model.IStocksProvider
import com.sec.android.app.shealth.network.data.Quote
import javax.inject.Inject

class HomeViewModel : ViewModel() {

  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var appPreferences: AppPreferences

  init {
    Injector.appComponent.inject(this)
  }

  val hasHoldings: Boolean
    get() = stocksProvider.hasPositions()

  fun getTotalHoldings(): String {
    var totalHoldings = 0.0
    if (stocksProvider.getTickers().isNotEmpty()) {
      totalHoldings = stocksProvider.getPortfolio().filter { it.hasPositions() }.sumOf { quote ->
        quote.holdings().toDouble()
      }
    }
    return appPreferences.selectedDecimalFormat.format(totalHoldings)
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
        "+" + appPreferences.selectedDecimalFormat.format(totalGain)
      } else {
        ""
      }

      totalLossStr = if (totalLoss != 0.0f) {
        appPreferences.selectedDecimalFormat.format(totalLoss)
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
    return stocksProvider.fetchState.displayString
  }

  fun nextFetch(): String {
    return stocksProvider.nextFetch()
  }
}
