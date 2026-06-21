package com.github.premnirmal.ticker.test

import com.github.premnirmal.ticker.model.FetchResult
import com.github.premnirmal.ticker.model.FetchState
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
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

    private var nextHoldingId = 1L

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

    override suspend fun addHolding(ticker: String, shares: Float, price: Float): Holding {
        val holding = Holding(symbol = ticker, shares = shares, price = price, id = nextHoldingId++)
        val quote = quotesBySymbol.getOrPut(ticker) { Quote(symbol = ticker) }
        val position = quote.position ?: Position(ticker).also { quote.position = it }
        position.add(holding)
        return holding
    }

    override suspend fun removePosition(ticker: String, holding: Holding): Boolean {
        return quotesBySymbol[ticker]?.position?.remove(holding) ?: false
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
