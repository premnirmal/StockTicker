package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider constructor(
    @ApplicationContext private val context: Context,
    private val api: StocksApi,
    private val preferences: SharedPreferences,
    private val clock: AppClock,
    private val appPreferences: AppPreferences,
    private val widgetDataProvider: WidgetDataProvider,
    private val alarmScheduler: AlarmScheduler,
    private val fetchEventLogger: FetchEventLogger,
    private val storage: StocksStorage,
    private val coroutineScope: CoroutineScope
) : IStocksProvider {

    companion object {
        private const val LAST_FETCHED = "LAST_FETCHED"
        private const val NEXT_FETCH = "NEXT_FETCH"
        private const val CONSECUTIVE_FETCH_FAILURES = "CONSECUTIVE_FETCH_FAILURES"
        private const val MIN_SCHEDULE_MS = 15_000L
        private const val MAX_FAILURE_BACKOFF_MS = 30 * 60 * 1000L
        private val DEFAULT_STOCKS = arrayOf("^GSPC", "^DJI", "GOOG", "AAPL", "MSFT")
        const val DEFAULT_INTERVAL_MS: Long = 15_000L
    }

    override val tickers: StateFlow<List<String>>
        get() = _tickers

    override val portfolio: StateFlow<List<Quote>>
        get() = _portfolio // quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }

    val fetchState: StateFlow<FetchState>
        get() = _fetchState

    override val nextFetchMs: StateFlow<Long>
        get() = _nextFetch

    private val tickerSet: MutableSet<String> = HashSet()
    private val quoteMap: MutableMap<String, Quote> = HashMap()
    private val _fetchState = MutableStateFlow<FetchState>(FetchState.NotFetched)
    private val _nextFetch = MutableStateFlow<Long>(0)
    private var lastFetched = 0L
    private val _tickers = MutableStateFlow<List<String>>(emptyList())
    private val _portfolio = MutableStateFlow<List<Quote>>(emptyList())

    init {
        val tickers = storage.readTickers()
        val lastFetchedLoaded = preferences.getLong(LAST_FETCHED, 0L)
        lastFetched = lastFetchedLoaded
        val nextFetch = preferences.getLong(NEXT_FETCH, 0L)
        _nextFetch.value = nextFetch
        this.tickerSet.addAll(tickers)
        if (this.tickerSet.isEmpty()) {
            this.tickerSet.addAll(DEFAULT_STOCKS)
        }
        _tickers.value = tickerSet.toList()
        _fetchState.value = FetchState.Success(lastFetched)
        runBlocking { fetchLocal() }
        if (lastFetched == 0L || (nextFetch > 0L && nextFetch < clock.currentTimeMillis())) {
            coroutineScope.launch {
                fetch()
            }
        }
    }

    private suspend fun fetchLocal() = withContext(Dispatchers.IO) {
        try {
            val quotes = storage.readQuotes()
            synchronized(quoteMap) {
                quotes.forEach { quoteMap[it.symbol] = it }
            }
            _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        } catch (e: Exception) {
            Timber.w(e)
        }
    }

    private fun saveLastFetched(lastFetched: Long) {
        preferences.edit {
            putLong(LAST_FETCHED, lastFetched)
        }
    }

    private fun saveTickers() {
        storage.saveTickers(tickerSet)
    }

    override fun scheduleUpdate(reason: String) {
        val msToNextAlarm = alarmScheduler.msToNextAlarm(lastFetched)
        scheduleUpdateWithMs(msToNextAlarm, reason)
    }

    private fun scheduleUpdateWithMs(
        msToNextAlarm: Long,
        reason: String
    ) {
        val clampedDelayMs = msToNextAlarm.coerceAtLeast(MIN_SCHEDULE_MS)
        val updateTime = alarmScheduler.scheduleUpdate(clampedDelayMs, context)
        _nextFetch.value = updateTime.toInstant().toEpochMilli()
        preferences.edit {
            putLong(NEXT_FETCH, updateTime.toInstant().toEpochMilli())
        }
        Timber.d(
            "Scheduled next refresh reason=%s delayMs=%d at=%s",
            reason,
            clampedDelayMs,
            updateTime.toInstant()
        )
        logFetchEvent(
            event = "schedule_next",
            detail = "reason=$reason delayMs=$clampedDelayMs nextAt=${updateTime.toInstant()}"
        )
        appPreferences.setRefreshing(false)
    }

    private fun logFetchEvent(event: String, detail: String) =
        fetchEventLogger.log(source = "StocksProvider", event = event, detail = detail)

    private fun resetConsecutiveFailures() {
        preferences.edit { putInt(CONSECUTIVE_FETCH_FAILURES, 0) }
    }

    private fun incrementConsecutiveFailures(): Int {
        val nextValue = preferences.getInt(CONSECUTIVE_FETCH_FAILURES, 0) + 1
        preferences.edit { putInt(CONSECUTIVE_FETCH_FAILURES, nextValue) }
        return nextValue
    }

    private fun scheduleFailureBackoff(reason: String, error: Throwable?) {
        val failureCount = incrementConsecutiveFailures()
        val exponent = (failureCount - 1).coerceAtMost(10)
        val backoffMs = (MINUTES.toMillis(1) * (1L shl exponent)).coerceAtMost(MAX_FAILURE_BACKOFF_MS)
        val regularScheduleMs = alarmScheduler.msToNextAlarm(lastFetched)
        val retryDelayMs = minOf(regularScheduleMs, backoffMs)
        val scheduleReason = "failure_backoff($failureCount):$reason"
        if (error != null) {
            Timber.w(
                error,
                "Fetch failed reason=%s failures=%d backoffMs=%d regularMs=%d chosenMs=%d",
                reason,
                failureCount,
                backoffMs,
                regularScheduleMs,
                retryDelayMs
            )
        } else {
            Timber.w(
                "Fetch failed reason=%s failures=%d backoffMs=%d regularMs=%d chosenMs=%d",
                reason,
                failureCount,
                backoffMs,
                regularScheduleMs,
                retryDelayMs
            )
        }
        logFetchEvent(
            event = "failure_backoff",
            detail = "reason=$reason failures=$failureCount backoffMs=$backoffMs regularMs=$regularScheduleMs chosenMs=$retryDelayMs"
        )
        scheduleUpdateWithMs(retryDelayMs, scheduleReason)
    }

    private suspend fun fetchStockInternal(ticker: String, allowCache: Boolean): FetchResult<Quote> = withContext(
        Dispatchers.IO
    ) {
        val quote = if (allowCache) quoteMap[ticker] else null
        return@withContext quote?.let { FetchResult.success(quote) } ?: run {
            try {
                val res = api.getStock(ticker)
                if (res.wasSuccessful) {
                    val data = res.data
                    val quoteFromStorage = storage.readQuote(ticker)
                    val quote = quoteFromStorage?.let {
                        it.copyValues(data)
                        quoteFromStorage
                    } ?: data
                    quoteMap[ticker] = quote
                    FetchResult.success(quote)
                } else {
                    FetchResult.failure<Quote>(FetchException("Failed to fetch", res.error))
                }
            } catch (ex: CancellationException) {
                // ignore
                FetchResult.failure<Quote>(FetchException("Failed to fetch", ex))
            } catch (ex: Exception) {
                Timber.w(ex)
                FetchResult.failure<Quote>(FetchException("Failed to fetch", ex))
            }
        }
    }

    // ///////////////////
    // public api
    // ///////////////////

    override fun hasTicker(ticker: String): Boolean {
        return tickerSet.contains(ticker)
    }

    override suspend fun fetch(allowScheduling: Boolean): FetchResult<List<Quote>> = withContext(Dispatchers.IO) {
        if (tickerSet.isEmpty()) {
            Timber.d("No tickers/symbols to fetch")
            FetchResult.failure<List<Quote>>(FetchException("No symbols in portfolio"))
        } else {
            var shouldScheduleInFinally = allowScheduling
            var failureReason = "unknown"
            var failureError: Throwable? = null
            return@withContext try {
                Timber.d("Starting fetch allowScheduling=%s tickers=%d", allowScheduling, tickerSet.size)
                logFetchEvent(
                    event = "fetch_start",
                    detail = "allowScheduling=$allowScheduling tickers=${tickerSet.size}"
                )
                if (allowScheduling) {
                    appPreferences.setRefreshing(true)
                }
                val fr = api.getStocks(tickerSet.toList())
                if (fr.hasError) {
                    failureReason = "api_error"
                    throw fr.error
                }
                val fetchedStocks = fr.data
                if (fetchedStocks.isEmpty()) {
                    failureReason = "empty_response"
                    failureError = FetchException("Refresh failed")
                    return@withContext FetchResult.failure<List<Quote>>(FetchException("Refresh failed"))
                } else {
                    synchronized(tickerSet) {
                        tickerSet.addAll(fetchedStocks.map { it.symbol })
                    }
                    _tickers.emit(tickerSet.toList())
                    storage.saveQuotes(fetchedStocks)
                    fetchLocal()
                    if (allowScheduling) {
                        lastFetched = clock.currentTimeMillis()
                        _fetchState.emit(FetchState.Success(lastFetched))
                        saveLastFetched(lastFetched)
                        resetConsecutiveFailures()
                        scheduleUpdate(reason = "fetch_success")
                        shouldScheduleInFinally = false
                    }
                    appPreferences.setRefreshing(false)
                    widgetDataProvider.broadcastUpdateAllWidgets()
                    Timber.d("Fetch succeeded stocks=%d", fetchedStocks.size)
                    logFetchEvent(
                        event = "fetch_success",
                        detail = "stocks=${fetchedStocks.size}"
                    )
                    FetchResult.success(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
                }
            } catch (ex: CancellationException) {
                shouldScheduleInFinally = false
                failureReason = "cancelled"
                failureError = ex
                FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex))
            } catch (ex: Throwable) {
                failureReason = ex::class.java.simpleName
                failureError = ex
                Timber.w(ex)
                FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex))
            } finally {
                appPreferences.setRefreshing(false)
                if (shouldScheduleInFinally) {
                    // Keep scheduling chain alive across transient failures.
                    logFetchEvent(
                        event = "fetch_failure",
                        detail = "reason=$failureReason error=${failureError?.message.orEmpty()}"
                    )
                    runCatching { scheduleFailureBackoff(failureReason, failureError) }
                        .onFailure { Timber.w(it, "Failed scheduling after fetch failure") }
                }
            }
        }
    }

    override fun schedule() {
        coroutineScope.launch {
            scheduleUpdate()
            alarmScheduler.enqueuePeriodicRefresh()
            alarmScheduler.enqueuePeriodicCleanup()
        }
    }

    override fun addStock(ticker: String): Collection<String> {
        synchronized(quoteMap) {
            if (!tickerSet.contains(ticker)) {
                tickerSet.add(ticker)
                val quote = Quote(symbol = ticker)
                quoteMap[ticker] = quote
                saveTickers()
            }
        }
        _tickers.value = tickerSet.toList()
        _portfolio.value = quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList()
        coroutineScope.launch {
            val result = fetchStockInternal(ticker, false)
            if (result.wasSuccessful) {
                val data = result.data
                quoteMap[ticker] = data
                storage.saveQuote(result.data)
                _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
            }
        }
        return tickerSet
    }

    override fun hasPositions(): Boolean = quoteMap.filter { it.value.hasPositions() }.isNotEmpty()

    override fun hasPosition(ticker: String): Boolean = quoteMap[ticker]?.hasPositions() ?: false

    override fun getPosition(ticker: String): Position? = quoteMap[ticker]?.position

    override suspend fun addHolding(
        ticker: String,
        shares: Float,
        price: Float
    ): Holding {
        val quote: Quote?
        var position: Position
        synchronized(quoteMap) {
            quote = quoteMap[ticker]
            position = getPosition(ticker) ?: Position(ticker)
            if (!tickerSet.contains(ticker)) {
                tickerSet.add(ticker)
            }
        }
        _tickers.emit(tickerSet.toList())
        saveTickers()
        val holding = Holding(ticker, shares, price)
        position.add(holding)
        quote?.position = position
        val id = storage.addHolding(holding)
        holding.id = id
        _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        return holding
    }

    override suspend fun removePosition(
        ticker: String,
        holding: Holding
    ): Boolean {
        var removed = false
        synchronized(quoteMap) {
            val position = getPosition(ticker)
            val quote = quoteMap[ticker]
            removed = position?.remove(holding) ?: false
            quote?.position = position
        }
        storage.removeHolding(ticker, holding)
        _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        return removed
    }

    override fun addStocks(symbols: Collection<String>): Collection<String> {
        synchronized(this.tickerSet) {
            val filterNot = symbols.filterNot { this.tickerSet.contains(it) }
            filterNot.forEach { this.tickerSet.add(it) }
            saveTickers()
            if (filterNot.isNotEmpty()) {
                coroutineScope.launch {
                    fetch()
                }
            }
        }
        _tickers.value = tickerSet.toList()
        _portfolio.value = quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList()
        return this.tickerSet
    }

    override suspend fun removeStock(ticker: String): Collection<String> {
        synchronized(quoteMap) {
            tickerSet.remove(ticker)
            saveTickers()
            quoteMap.remove(ticker)
        }
        storage.removeQuoteBySymbol(ticker)
        _tickers.emit(tickerSet.toList())
        _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        return tickerSet
    }

    override suspend fun removeStocks(symbols: Collection<String>) {
        synchronized(quoteMap) {
            symbols.forEach {
                tickerSet.remove(it)
                quoteMap.remove(it)
            }
        }
        storage.removeQuotesBySymbol(symbols.toList())
        _tickers.emit(tickerSet.toList())
        _portfolio.emit(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
        saveTickers()
    }

    override suspend fun cleanup() {
        val quotes = storage.readQuotes().map { it.symbol }
        val toRemove = quotes.filterNot {
            tickerSet.contains(it)
        }
        storage.removeQuotesBySymbol(toRemove)
    }

    override suspend fun fetchStock(ticker: String, allowCache: Boolean): FetchResult<Quote> {
        return fetchStockInternal(ticker, allowCache)
    }

    override fun getStock(ticker: String): Quote? = quoteMap[ticker]

    override fun addPortfolio(portfolio: List<Quote>) {
        synchronized(quoteMap) {
            portfolio.forEach {
                val symbol = it.symbol
                if (!tickerSet.contains(symbol)) tickerSet.add(symbol)
                quoteMap[symbol] = it
            }
        }
        saveTickers()
        widgetDataProvider.updateWidgets(tickerSet.toList())
        coroutineScope.launch {
            storage.saveQuotes(portfolio)
            fetchLocal()
            fetch()
        }
    }

    sealed class FetchState {
        abstract val displayString: String

        object NotFetched : FetchState() {
            override val displayString: String = "--"
        }

        class Success(val fetchTime: Long) : FetchState() {
            override val displayString: String by lazy {
                val instant = Instant.ofEpochMilli(fetchTime)
                val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
                time.createTimeString()
            }
        }

        class Failure(val exception: Throwable) : FetchState() {
            override val displayString: String by lazy {
                exception.message.orEmpty()
            }
        }
    }
}
