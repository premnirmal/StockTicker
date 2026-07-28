package com.github.premnirmal.ticker.test

import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.FetchState
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.RemoveMovementResult
import com.github.premnirmal.ticker.model.SellResult
import com.github.premnirmal.ticker.network.data.Movement
import com.github.premnirmal.ticker.network.data.MovementType
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.network.data.SHARE_EPSILON
import com.github.premnirmal.ticker.network.data.isValidLedger
import com.github.premnirmal.ticker.network.data.replayLedger
import com.github.premnirmal.ticker.network.data.toPosition
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [IStocksProvider] used by the shared ViewModel tests. It keeps the watchlist, the cached
 * quotes and their positions in plain maps so tests can drive the providers deterministically without
 * any platform persistence or networking.
 */
class FakeStocksProvider(
    quotes: List<Quote> = emptyList()
) : IStocksProvider {

    private val quotesBySymbol: MutableMap<String, Quote> =
        quotes.associateBy { it.symbol }.toMutableMap()

    /** Result returned by [fetchStock]; defaults to the cached quote (or a failure when absent). */
    var fetchStockResult: ((String) -> FetchResult<Quote>)? = null

    private val _tickers = MutableStateFlow(quotesBySymbol.keys.toList())
    override val tickers: StateFlow<List<String>> = _tickers

    private val _fetchState = MutableStateFlow<FetchState>(FetchState.NotFetched)
    override val fetchState: StateFlow<FetchState> = _fetchState

    private val _portfolio = MutableStateFlow(quotesBySymbol.values.toList())
    override val portfolio: StateFlow<List<Quote>> = _portfolio

    private val _nextFetchMs = MutableStateFlow(0L)
    override val nextFetchMs: StateFlow<Long> = _nextFetchMs

    private val movementsBySymbol: MutableMap<String, MutableList<Movement>> = mutableMapOf()
    private var nextMovementId = 1L

    fun setQuote(quote: Quote) {
        quotesBySymbol[quote.symbol] = quote
        _tickers.value = quotesBySymbol.keys.toList()
        _portfolio.value = quotesBySymbol.values.toList()
    }

    override fun scheduleUpdate(reason: String) = Unit

    override fun hasTicker(ticker: String): Boolean = quotesBySymbol.containsKey(ticker)

    override suspend fun fetch(allowScheduling: Boolean): FetchResult<List<Quote>> =
        FetchResult.success(quotesBySymbol.values.toList())

    override fun schedule() = Unit

    override fun addStock(ticker: String): Collection<String> {
        if (!quotesBySymbol.containsKey(ticker)) {
            quotesBySymbol[ticker] = Quote(symbol = ticker)
            _tickers.value = quotesBySymbol.keys.toList()
        }
        return quotesBySymbol.keys
    }

    override fun hasPositions(): Boolean =
        quotesBySymbol.values.any { it.position?.holdings?.isNotEmpty() == true }

    override fun hasPosition(ticker: String): Boolean =
        quotesBySymbol[ticker]?.position?.holdings?.isNotEmpty() == true

    override fun getPosition(ticker: String): Position? = quotesBySymbol[ticker]?.position

    override fun getMovements(ticker: String): List<Movement> =
        movementsBySymbol[ticker] ?: emptyList()

    override suspend fun buy(ticker: String, shares: Float, price: Float): Movement {
        val movement = Movement(ticker, MovementType.BUY, shares, price, id = nextMovementId++)
        val movements = movementsBySymbol.getOrPut(ticker) { mutableListOf() }
        movements.add(movement)
        refreshLedger(ticker)
        return movement
    }

    override suspend fun sell(ticker: String, shares: Float, price: Float): SellResult {
        val summary = getMovements(ticker).replayLedger()
        if (shares > summary.shares + SHARE_EPSILON) {
            return SellResult.NotEnoughShares(summary.shares)
        }
        val movement = Movement(ticker, MovementType.SELL, shares, price, id = nextMovementId++)
        val movements = movementsBySymbol.getOrPut(ticker) { mutableListOf() }
        movements.add(movement)
        refreshLedger(ticker)
        return SellResult.Success(movement, (price - summary.averagePrice) * shares)
    }

    override suspend fun removeMovement(ticker: String, movement: Movement): RemoveMovementResult {
        val remaining = getMovements(ticker).filterNot { it.id == movement.id }
        if (!remaining.isValidLedger()) return RemoveMovementResult.BlockedBySells
        movementsBySymbol[ticker] = remaining.toMutableList()
        refreshLedger(ticker)
        return RemoveMovementResult.Removed
    }

    private fun refreshLedger(ticker: String) {
        val movements = getMovements(ticker)
        val quote = quotesBySymbol.getOrPut(ticker) { Quote(symbol = ticker) }
        quote.movements = movements
        quote.position = movements.replayLedger().toPosition(ticker)
        _portfolio.value = quotesBySymbol.values.toList()
    }

    override fun addStocks(symbols: Collection<String>): Collection<String> {
        symbols.forEach { addStock(it) }
        return quotesBySymbol.keys
    }

    override suspend fun removeStock(ticker: String): Collection<String> {
        quotesBySymbol.remove(ticker)
        _tickers.value = quotesBySymbol.keys.toList()
        _portfolio.value = quotesBySymbol.values.toList()
        return quotesBySymbol.keys
    }

    override suspend fun removeStocks(symbols: Collection<String>) {
        symbols.forEach { quotesBySymbol.remove(it) }
        _tickers.value = quotesBySymbol.keys.toList()
        _portfolio.value = quotesBySymbol.values.toList()
    }

    override suspend fun cleanup() = Unit

    override suspend fun fetchStock(ticker: String, allowCache: Boolean): FetchResult<Quote> {
        fetchStockResult?.let { return it(ticker) }
        val quote = quotesBySymbol[ticker]
        return if (quote != null) {
            FetchResult.success(quote)
        } else {
            FetchResult.failure(IllegalStateException("No quote for $ticker"))
        }
    }

    override fun getStock(ticker: String): Quote? = quotesBySymbol[ticker]

    override fun addPortfolio(portfolio: List<Quote>) {
        portfolio.forEach { quotesBySymbol[it.symbol] = it }
        _tickers.value = quotesBySymbol.keys.toList()
        _portfolio.value = quotesBySymbol.values.toList()
    }
}
