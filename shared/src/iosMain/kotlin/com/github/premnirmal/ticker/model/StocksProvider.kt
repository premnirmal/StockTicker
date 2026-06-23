package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.UserDefaultsPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AppLogger
import com.github.premnirmal.ticker.components.ioDispatcher
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.settings.PreferenceStore
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import platform.Foundation.NSRecursiveLock

/**
 * iOS implementation of the shared [IStocksProvider] contract: the central observable
 * watchlist/portfolio state plus the add/remove/fetch/schedule operations.
 *
 * It is the iOS counterpart of Android's `StocksProvider`, wired to the multiplatform infrastructure
 * instead of Android types: the shared [StocksApi] for network, the Room-backed [StocksStorage] for
 * persistence, [BackgroundRefreshScheduler] for background scheduling, the shared [FetchEventLogger] for
 * diagnostics and [PreferenceStore]/[UserDefaultsPreferences] for settings. Where Android broadcasts to
 * its app widgets, iOS invokes the [onQuotesUpdated] hook so the app can reload its WidgetKit
 * timelines.
 *
 * The refresh state is exposed through the shared [fetchState] flow; its [FetchState] display string
 * is formatted behind the multiplatform [formatFetchTime] boundary (`kotlinx-datetime` on iOS).
 */
class StocksProvider(
    private val api: StocksApi,
    private val storage: StocksStorage,
    private val scheduler: BackgroundRefreshScheduler,
    private val appPreferences: UserDefaultsPreferences,
    private val fetchEventLogger: FetchEventLogger,
    private val clock: AppClock,
    private val store: PreferenceStore,
    private val coroutineScope: CoroutineScope,
    private val onQuotesUpdated: () -> Unit = {}
) : IStocksProvider {

    private val lock = NSRecursiveLock()
    private val tickerSet: MutableSet<String> = mutableSetOf()
    private val quoteMap: MutableMap<String, Quote> = mutableMapOf()
    private var lastFetched = 0L

    private val _tickers = MutableStateFlow<List<String>>(emptyList())
    private val _portfolio = MutableStateFlow<List<Quote>>(emptyList())
    private val _nextFetch = MutableStateFlow(0L)
    private val _fetchState = MutableStateFlow<FetchState>(FetchState.NotFetched)

    override val tickers: StateFlow<List<String>> get() = _tickers
    override val portfolio: StateFlow<List<Quote>> get() = _portfolio
    override val nextFetchMs: StateFlow<Long> get() = _nextFetch
    override val fetchState: StateFlow<FetchState> get() = _fetchState

    init {
        val saved = storage.readTickers()
        lastFetched = store.getLong(LAST_FETCHED, 0L)
        _nextFetch.value = store.getLong(NEXT_FETCH, 0L)
        tickerSet.addAll(saved)
        if (tickerSet.isEmpty()) {
            tickerSet.addAll(DEFAULT_STOCKS)
        }
        _tickers.value = tickerSet.toList()
        _fetchState.value = FetchState.Success(lastFetched)
        coroutineScope.launch {
            fetchLocal()
            val nextFetch = _nextFetch.value
            if (lastFetched == 0L || (nextFetch > 0L && nextFetch < clock.currentTimeMillis())) {
                fetch()
            }
        }
        // Re-emit the portfolio whenever the auto-sort preference changes so the watchlist
        // re-orders immediately when the user toggles it in Settings.
        coroutineScope.launch {
            appPreferences.autoSortFlow.collect { emitPortfolio() }
        }
    }

    private suspend fun fetchLocal() = withContext(ioDispatcher) {
        try {
            val quotes = storage.readQuotes()
            lock.withLock {
                quotes.forEach { quoteMap[it.symbol] = it }
            }
            emitPortfolio()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            AppLogger.w(e, "Failed to read local quotes")
        }
    }

    private fun emitPortfolio() {
        val quotes = quoteMap.values.filter { tickerSet.contains(it.symbol) }
        _portfolio.value = if (appPreferences.autoSort()) {
            quotes.sortedByDescending { it.changeInPercent }
        } else {
            quotes.toList()
        }
    }

    private fun saveTickers() = storage.saveTickers(tickerSet)

    private fun logFetchEvent(event: String, detail: String) =
        fetchEventLogger.log(source = "StocksProvider", event = event, detail = detail)

    override fun scheduleUpdate(reason: String) {
        val msToNextAlarm = scheduler.msToNextAlarm(lastFetched)
        scheduleUpdateWithMs(msToNextAlarm, reason)
    }

    private fun scheduleUpdateWithMs(msToNextAlarm: Long, reason: String) {
        val clampedDelayMs = msToNextAlarm.coerceAtLeast(MIN_SCHEDULE_MS)
        scheduler.scheduleRefresh(clampedDelayMs)
        val nextAt = clock.currentTimeMillis() + clampedDelayMs
        _nextFetch.value = nextAt
        store.setLong(NEXT_FETCH, nextAt)
        logFetchEvent(event = "schedule_next", detail = "reason=$reason delayMs=$clampedDelayMs nextAt=$nextAt")
        appPreferences.setRefreshing(false)
    }

    private fun resetConsecutiveFailures() = store.setInt(CONSECUTIVE_FETCH_FAILURES, 0)

    private fun incrementConsecutiveFailures(): Int {
        val nextValue = store.getInt(CONSECUTIVE_FETCH_FAILURES, 0) + 1
        store.setInt(CONSECUTIVE_FETCH_FAILURES, nextValue)
        return nextValue
    }

    private fun scheduleFailureBackoff(reason: String) {
        val failureCount = incrementConsecutiveFailures()
        val exponent = (failureCount - 1).coerceAtMost(10)
        val backoffMs = (MILLIS_PER_MINUTE * (1L shl exponent)).coerceAtMost(MAX_FAILURE_BACKOFF_MS)
        val regularScheduleMs = scheduler.msToNextAlarm(lastFetched)
        val retryDelayMs = minOf(regularScheduleMs, backoffMs)
        logFetchEvent(
            event = "failure_backoff",
            detail = "reason=$reason failures=$failureCount backoffMs=$backoffMs regularMs=$regularScheduleMs chosenMs=$retryDelayMs"
        )
        scheduleUpdateWithMs(retryDelayMs, "failure_backoff($failureCount):$reason")
    }

    private suspend fun fetchStockInternal(ticker: String, allowCache: Boolean): FetchResult<Quote> =
        withContext(ioDispatcher) {
            val cached = if (allowCache) quoteMap[ticker] else null
            cached?.let { return@withContext FetchResult.success(it) }
            try {
                val res = api.getStock(ticker)
                if (res.wasSuccessful) {
                    val data = res.data
                    val quoteFromStorage = storage.readQuote(ticker)
                    val quote = quoteFromStorage?.let {
                        it.copyValues(data)
                        it
                    } ?: data
                    quoteMap[ticker] = quote
                    FetchResult.success(quote)
                } else {
                    FetchResult.failure(FetchException("Failed to fetch", res.error))
                }
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Exception) {
                AppLogger.w(ex, "Failed to fetch $ticker")
                FetchResult.failure(FetchException("Failed to fetch", ex))
            }
        }

    override fun hasTicker(ticker: String): Boolean = tickerSet.contains(ticker)

    override suspend fun fetch(allowScheduling: Boolean): FetchResult<List<Quote>> = withContext(ioDispatcher) {
        if (tickerSet.isEmpty()) {
            return@withContext FetchResult.failure(FetchException("No symbols in portfolio"))
        }
        var shouldScheduleInFinally = allowScheduling
        var failureReason = "unknown"
        try {
            logFetchEvent(event = "fetch_start", detail = "allowScheduling=$allowScheduling tickers=${tickerSet.size}")
            if (allowScheduling) appPreferences.setRefreshing(true)
            val fr = api.getStocks(tickerSet.toList())
            if (fr.hasError) {
                failureReason = "api_error"
                throw fr.error
            }
            val fetchedStocks = fr.data
            if (fetchedStocks.isEmpty()) {
                return@withContext FetchResult.failure(FetchException("Refresh failed"))
            }
            lock.withLock {
                tickerSet.addAll(fetchedStocks.map { it.symbol })
            }
            _tickers.value = tickerSet.toList()
            storage.saveQuotes(fetchedStocks)
            fetchLocal()
            if (allowScheduling) {
                lastFetched = clock.currentTimeMillis()
                store.setLong(LAST_FETCHED, lastFetched)
                _fetchState.value = FetchState.Success(lastFetched)
                resetConsecutiveFailures()
                scheduleUpdate(reason = "fetch_success")
                shouldScheduleInFinally = false
            }
            appPreferences.setRefreshing(false)
            onQuotesUpdated()
            logFetchEvent(event = "fetch_success", detail = "stocks=${fetchedStocks.size}")
            FetchResult.success(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        } catch (ex: CancellationException) {
            shouldScheduleInFinally = false
            throw ex
        } catch (ex: Throwable) {
            failureReason = ex::class.simpleName ?: "Throwable"
            AppLogger.w(ex, "Fetch failed")
            FetchResult.failure(FetchException("Failed to fetch", ex))
        } finally {
            appPreferences.setRefreshing(false)
            if (shouldScheduleInFinally) {
                logFetchEvent(event = "fetch_failure", detail = "reason=$failureReason")
                runCatching { scheduleFailureBackoff(failureReason) }
                    .onFailure { AppLogger.w(it, "Failed scheduling after fetch failure") }
            }
        }
    }

    override fun schedule() {
        coroutineScope.launch {
            try {
                scheduleUpdate()
                scheduler.enqueuePeriodicRefresh()
                scheduler.enqueuePeriodicCleanup()
            } catch (ex: CancellationException) {
                throw ex
            } catch (ex: Throwable) {
                AppLogger.w(ex, "Failed to schedule background refresh")
            }
        }
    }

    override fun addStock(ticker: String): Collection<String> {
        lock.withLock {
            if (!tickerSet.contains(ticker)) {
                tickerSet.add(ticker)
                quoteMap[ticker] = Quote(symbol = ticker)
                saveTickers()
            }
        }
        _tickers.value = tickerSet.toList()
        emitPortfolio()
        coroutineScope.launch {
            val result = fetchStockInternal(ticker, false)
            if (result.wasSuccessful) {
                quoteMap[ticker] = result.data
                storage.saveQuote(result.data)
                emitPortfolio()
            }
        }
        return tickerSet
    }

    override fun hasPositions(): Boolean = quoteMap.values.any { it.hasPositions() }

    override fun hasPosition(ticker: String): Boolean = quoteMap[ticker]?.hasPositions() ?: false

    override fun getPosition(ticker: String): Position? = quoteMap[ticker]?.position

    override suspend fun addHolding(ticker: String, shares: Float, price: Float): Holding {
        val (quote, position) = lock.withLock {
            val q = quoteMap[ticker]
            val p = getPosition(ticker) ?: Position(ticker)
            if (!tickerSet.contains(ticker)) tickerSet.add(ticker)
            q to p
        }
        _tickers.value = tickerSet.toList()
        saveTickers()
        val holding = Holding(ticker, shares, price)
        position.add(holding)
        quote?.position = position
        holding.id = storage.addHolding(holding)
        emitPortfolio()
        return holding
    }

    override suspend fun removePosition(ticker: String, holding: Holding): Boolean {
        var removed = false
        lock.withLock {
            val position = getPosition(ticker)
            val quote = quoteMap[ticker]
            removed = position?.remove(holding) ?: false
            quote?.position = position
        }
        storage.removeHolding(ticker, holding)
        emitPortfolio()
        return removed
    }

    override fun addStocks(symbols: Collection<String>): Collection<String> {
        lock.withLock {
            val filterNot = symbols.filterNot { tickerSet.contains(it) }
            filterNot.forEach { tickerSet.add(it) }
            saveTickers()
            if (filterNot.isNotEmpty()) {
                coroutineScope.launch { fetch() }
            }
        }
        _tickers.value = tickerSet.toList()
        emitPortfolio()
        return tickerSet
    }

    override suspend fun removeStock(ticker: String): Collection<String> {
        lock.withLock {
            tickerSet.remove(ticker)
            saveTickers()
            quoteMap.remove(ticker)
        }
        storage.removeQuoteBySymbol(ticker)
        _tickers.value = tickerSet.toList()
        emitPortfolio()
        return tickerSet
    }

    override suspend fun removeStocks(symbols: Collection<String>) {
        lock.withLock {
            symbols.forEach {
                tickerSet.remove(it)
                quoteMap.remove(it)
            }
        }
        storage.removeQuotesBySymbol(symbols.toList())
        _tickers.value = tickerSet.toList()
        emitPortfolio()
        saveTickers()
    }

    override suspend fun cleanup() {
        val quotes = storage.readQuotes().map { it.symbol }
        val toRemove = quotes.filterNot { tickerSet.contains(it) }
        storage.removeQuotesBySymbol(toRemove)
    }

    override suspend fun fetchStock(ticker: String, allowCache: Boolean): FetchResult<Quote> =
        fetchStockInternal(ticker, allowCache)

    override fun getStock(ticker: String): Quote? = quoteMap[ticker]

    override fun addPortfolio(portfolio: List<Quote>) {
        lock.withLock {
            portfolio.forEach {
                if (!tickerSet.contains(it.symbol)) tickerSet.add(it.symbol)
                quoteMap[it.symbol] = it
            }
        }
        saveTickers()
        _tickers.value = tickerSet.toList()
        onQuotesUpdated()
        coroutineScope.launch {
            storage.saveQuotes(portfolio)
            fetchLocal()
            fetch()
        }
    }

    companion object {
        private const val LAST_FETCHED = "LAST_FETCHED"
        private const val NEXT_FETCH = "NEXT_FETCH"
        private const val CONSECUTIVE_FETCH_FAILURES = "CONSECUTIVE_FETCH_FAILURES"
        private const val MIN_SCHEDULE_MS = 15_000L
        private const val MILLIS_PER_MINUTE = 60_000L
        private const val MAX_FAILURE_BACKOFF_MS = 30 * 60 * 1000L
        private val DEFAULT_STOCKS = arrayOf("^GSPC", "^DJI", "GOOG", "AAPL", "MSFT")
    }
}

private inline fun <T> NSRecursiveLock.withLock(block: () -> T): T {
    lock()
    try {
        return block()
    } finally {
        unlock()
    }
}
