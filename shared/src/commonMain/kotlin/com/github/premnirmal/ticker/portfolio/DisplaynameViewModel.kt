package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Properties
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.QuoteStorage
import kotlinx.coroutines.launch

class DisplaynameViewModel constructor(
    private val stocksProvider: IStocksProvider,
    private val stocksStorage: QuoteStorage
) : ViewModel() {

    lateinit var symbol: String
    val quote: Quote?
        get() = stocksProvider.getStock(symbol)

    fun setDisplayname(displaynameText: String) {
        viewModelScope.launch {
            quote?.let {
                val properties = it.properties ?: Properties(
                    symbol
                )
                it.properties = properties
                properties.displayname = displaynameText
                stocksStorage.saveQuoteProperties(properties)
            }
        }
    }
}
