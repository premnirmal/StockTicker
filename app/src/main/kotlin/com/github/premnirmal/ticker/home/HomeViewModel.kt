package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.Dispatchers
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

class HomeViewModel : ViewModel() {

  @Inject lateinit var stocksProvider: IStocksProvider
  @Inject lateinit var appPreferences: AppPreferences

  init {
    Injector.appComponent.inject(this)
  }

  val fetchState = stocksProvider.fetchState.asLiveData(Dispatchers.Main)
  val hasHoldings: Boolean
    get() = stocksProvider.hasPositions()

  fun getTotalHoldings(): String {
    var totalHoldings = 0.0
    if (stocksProvider.tickers.value.isNotEmpty()) {
      totalHoldings = stocksProvider.portfolio.value.filter { it.hasPositions() }.sumOf { quote ->
        quote.holdings().toDouble()
      }
    }
    return appPreferences.selectedDecimalFormat.format(totalHoldings)
  }

  fun getTotalGainLoss(): Pair<String, String> {
    var totalGainStr = ""
    var totalLossStr = ""

    if (stocksProvider.tickers.value.isNotEmpty()) {
      var totalGain = 0.0f
      var totalLoss = 0.0f
      val portfolio: List<Quote> = stocksProvider.portfolio.value

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
    stocksProvider.fetch().collect { fetch ->
      emit(fetch.wasSuccessful)
    }
  }

  fun lastFetched(): String {
    return stocksProvider.fetchState.value.displayString
  }

  fun nextFetch(): String {
    val nextUpdateMs = stocksProvider.nextFetchMs.value
    val instant = Instant.ofEpochMilli(nextUpdateMs)
    val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    return time.createTimeString()
  }
}
