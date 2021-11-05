package com.sec.android.app.shealth.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.model.IStocksProvider
import com.sec.android.app.shealth.network.data.Properties
import com.sec.android.app.shealth.network.data.Quote
import com.sec.android.app.shealth.repo.StocksStorage
import kotlinx.coroutines.launch
import javax.inject.Inject

class AlertsViewModel : ViewModel() {

  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var stocksStorage: StocksStorage

  lateinit var symbol: String
  val quote: Quote?
    get() = stocksProvider.getStock(symbol)

  init {
    Injector.appComponent.inject(this)
  }

  fun setAlerts(
    alertAbove: Float,
    alertBelow: Float
  ) {
    viewModelScope.launch {
      quote?.let {
        val properties = it.properties ?: Properties(
            symbol
        )
        it.properties = properties.apply {
          this.alertAbove = alertAbove
          this.alertBelow = alertBelow
        }
        stocksStorage.saveQuoteProperties(properties)
      }
    }
  }
}