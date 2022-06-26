package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import kotlinx.coroutines.launch
import javax.inject.Inject

class AddPositionViewModel : ViewModel() {

  @Inject internal lateinit var stocksProvider: IStocksProvider

  init {
    Injector.appComponent.inject(this)
  }

  val quote: LiveData<Quote>
    get() = _quote
  private val _quote = MutableLiveData<Quote>()

  fun getPosition(symbol: String): Position? {
    return stocksProvider.getPosition(symbol)
  }

  fun loadQuote(symbol: String) = viewModelScope.launch {
    _quote.value = getQuote(symbol)
  }

  fun removePosition(symbol: String, holding: Holding) {
    viewModelScope.launch {
      stocksProvider.removePosition(symbol, holding)
      loadQuote(symbol)
    }
  }

  fun addHolding(symbol: String, shares: Float, price: Float) = liveData {
    val holding = stocksProvider.addHolding(symbol, shares, price)
    emit(holding)
    loadQuote(symbol)
  }

  private fun getQuote(symbol: String): Quote {
    return checkNotNull(stocksProvider.getStock(symbol))
  }
}