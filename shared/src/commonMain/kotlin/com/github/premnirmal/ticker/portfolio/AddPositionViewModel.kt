package com.github.premnirmal.ticker.portfolio

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.RemoveMovementResult
import com.github.premnirmal.ticker.model.SellResult
import com.github.premnirmal.ticker.network.data.LedgerSummary
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.replayLedger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

sealed class PositionEvent {
    data class Bought(val movement: Movement) : PositionEvent()
    data class Sold(val movement: Movement, val gain: Float) : PositionEvent()
    data class SellRejected(val sharesOwned: Float) : PositionEvent()
    data class MovementRemoved(val movement: Movement) : PositionEvent()
    data object RemoveBlocked : PositionEvent()
}

class AddPositionViewModel constructor(private val stocksProvider: IStocksProvider) : ViewModel() {

    val quote: StateFlow<Quote?> get() = _quote
    private val _quote = MutableStateFlow<Quote?>(null)

    val movements: StateFlow<List<Movement>> get() = _movements
    private val _movements = MutableStateFlow<List<Movement>>(emptyList())

    val summary: StateFlow<LedgerSummary> get() = _summary
    private val _summary = MutableStateFlow(emptyList<Movement>().replayLedger())

    val events: Flow<PositionEvent> get() = _events
    private val _events = MutableSharedFlow<PositionEvent>()

    fun loadQuote(symbol: String) {
        viewModelScope.launch { loadInternal(symbol) }
    }

    fun buy(symbol: String, shares: Float, price: Float) {
        viewModelScope.launch {
            val movement = stocksProvider.buy(symbol, shares, price)
            loadInternal(symbol)
            _events.emit(PositionEvent.Bought(movement))
        }
    }

    fun sell(symbol: String, shares: Float, price: Float) {
        viewModelScope.launch {
            when (val result = stocksProvider.sell(symbol, shares, price)) {
                is SellResult.Success -> {
                    loadInternal(symbol)
                    _events.emit(PositionEvent.Sold(result.movement, result.gain))
                }
                is SellResult.NotEnoughShares ->
                    _events.emit(PositionEvent.SellRejected(result.sharesOwned))
            }
        }
    }

    fun deleteMovement(symbol: String, movement: Movement) {
        viewModelScope.launch {
            when (stocksProvider.removeMovement(symbol, movement)) {
                is RemoveMovementResult.Removed -> {
                    loadInternal(symbol)
                    _events.emit(PositionEvent.MovementRemoved(movement))
                }
                is RemoveMovementResult.BlockedBySells ->
                    _events.emit(PositionEvent.RemoveBlocked)
            }
        }
    }

    private fun loadInternal(symbol: String) {
        _quote.value = stocksProvider.getStock(symbol)
        val movements = stocksProvider.getMovements(symbol)
        _movements.value = movements
        _summary.value = movements.replayLedger()
    }
}
