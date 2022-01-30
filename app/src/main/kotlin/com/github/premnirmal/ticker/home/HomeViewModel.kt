package com.github.premnirmal.ticker.home

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.IStocksProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
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

  fun getTotalGainLoss(): LiveData<TotalGainLoss> {
    val data = MutableLiveData<TotalGainLoss>()
    viewModelScope.launch {
      if (stocksProvider.tickers.value.isNotEmpty()) {
        stocksProvider.portfolio.collect {
          val totalHoldings = it.filter { it.hasPositions() }
              .sumOf { quote ->
                quote.holdings()
                    .toDouble()
              }
          val totalHoldingsStr = appPreferences.selectedDecimalFormat.format(totalHoldings)
          var totalGain = 0.0f
          var totalLoss = 0.0f
          for (quote in it) {
            if (quote.hasPositions()) {
              val gainLoss = quote.gainLoss()
              if (gainLoss > 0.0f) {
                totalGain += gainLoss
              } else {
                totalLoss += gainLoss
              }
            }
          }
          val totalGainStr = if (totalGain != 0.0f) {
            "+" + appPreferences.selectedDecimalFormat.format(totalGain)
          } else {
            ""
          }
          val totalLossStr = if (totalLoss != 0.0f) {
            appPreferences.selectedDecimalFormat.format(totalLoss)
          } else {
            ""
          }
          data.value = TotalGainLoss(totalHoldingsStr, totalGainStr, totalLossStr)
          cancel()
        }
      } else data.value = TotalGainLoss("", "", "")
    }
    return data
  }

  fun fetch() = liveData {
    stocksProvider.fetch()
        .collect { fetch ->
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

  data class TotalGainLoss(
    val holdings: String,
    val gain: String,
    val loss: String
  )
}
