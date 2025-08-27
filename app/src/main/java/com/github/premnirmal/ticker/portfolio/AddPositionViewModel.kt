package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddPositionViewModel @Inject constructor(private val stocksProvider: StocksProvider) : ViewModel() {

    val position: StateFlow<Position>
        get() = _position
    private val _position = MutableStateFlow(Position(""))
    val quote: StateFlow<Quote?>
        get() = _quote
    private val _quote = MutableStateFlow<Quote?>(null)

    val removedHolding: Flow<Holding>
        get() = _removedHolding
    private val _removedHolding = MutableSharedFlow<Holding>()

    val addedHolding: Flow<Holding>
        get() = _addedHolding
    private val _addedHolding = MutableSharedFlow<Holding>()

    fun loadQuote(symbol: String) {
        viewModelScope.launch {
            loadQuoteInternal(symbol)
        }
    }

    fun removeHolding(symbol: String, holding: Holding) {
        viewModelScope.launch {
            val removed = stocksProvider.removePosition(symbol, holding)
            loadQuoteInternal(symbol)
            if (removed) {
                _removedHolding.emit(holding)
            }
        }
    }

    fun addHolding(symbol: String, shares: Float, price: Float) {
        viewModelScope.launch {
            val holding = stocksProvider.addHolding(symbol, shares, price)
            loadQuoteInternal(symbol)
            _addedHolding.emit(holding)
        }
    }

    private fun loadQuoteInternal(symbol: String) {
        _quote.value = getQuote(symbol)
        _position.value = getPosition(symbol)
    }

    private fun getQuote(symbol: String): Quote {
        return checkNotNull(stocksProvider.getStock(symbol))
    }

    private fun getPosition(symbol: String): Position {
        return stocksProvider.getPosition(symbol) ?: Position(symbol)
    }
}
