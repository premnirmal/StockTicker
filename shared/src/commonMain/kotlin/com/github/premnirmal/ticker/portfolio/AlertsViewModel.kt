package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.QuoteStorage
import kotlinx.coroutines.launch

class AlertsViewModel constructor(
    private val stocksProvider: IStocksProvider,
    private val stocksStorage: QuoteStorage
) : ViewModel() {

    lateinit var symbol: String
    val quote: Quote?
        get() = stocksProvider.getStock(symbol)

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
