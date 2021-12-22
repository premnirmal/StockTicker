package com.github.premnirmal.ticker.home

import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.liveData
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.model.IStocksProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
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
