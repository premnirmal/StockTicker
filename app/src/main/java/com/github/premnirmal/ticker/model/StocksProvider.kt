package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
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
import javax.inject.Singleton
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import com.github.premnirmal.ticker.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider constructor(
    @ApplicationContext private val context: Context,
    private val api: StocksApi,
    private val clock: AppClock,
    private val appPreferences: AppPreferences,
    private val widgetDataProvider: WidgetDataProvider,
    private val alarmScheduler: AlarmScheduler,
    private val storage: StocksStorage,
    private val coroutineScope: CoroutineScope
) {

    companion object {
        private const val LAST_FETCHED = "LAST_FETCHED"
        private const val NEXT_FETCH = "NEXT_FETCH"
        private val DEFAULT_STOCKS = arrayOf("^GSPC", "^DJI", "GOOG", "AAPL", "MSFT")
        const val DEFAULT_INTERVAL_MS: Long = 15_000L
    }

    val tickers: StateFlow<List<String>>
        get() = _tickers

    val portfolio: StateFlow<List<Quote>>
        get() = _portfolio // quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }

    val fetchState: StateFlow<FetchState>
        get() = lastFetched.map {
            if (it == 0L) {
                FetchState.NotFetched
            } else {
                FetchState.Success(it)
            }
        }.let {
            val initial = runBlocking { it.firstOrNull() }
            MutableStateFlow(initial ?: FetchState.NotFetched)
        }

    val nextFetchMs: StateFlow<Long>
        get() = context.dataStore.data.map {
            it[longPreferencesKey(NEXT_FETCH)] ?: 0L
        }.let {
            val initial = runBlocking { it.firstOrNull() }
            MutableStateFlow(initial ?: 0L)
        }

    private val lastFetched: Flow<Long>
        get() = context.dataStore.data.map {
            val lastFetchedLoaded = it[longPreferencesKey(LAST_FETCHED)] ?: 0L
            if (lastFetchedLoaded == 0L) {
                coroutineScope.launch {
                    fetch()
                }
            }
            lastFetchedLoaded
        }

    private val tickerSet: MutableSet<String> = HashSet()
    private val quoteMap: MutableMap<String, Quote> = HashMap()
    private val _tickers = MutableStateFlow<List<String>>(emptyList())
    private val _portfolio = MutableStateFlow<List<Quote>>(emptyList())

    init {
        val tickers = storage.readTickers()
        this.tickerSet.addAll(tickers)
        if (this.tickerSet.isEmpty()) {
            this.tickerSet.addAll(DEFAULT_STOCKS)
        }
        _tickers.value = tickerSet.toList()
        coroutineScope.launch { fetchLocal() }
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

    private suspend fun saveLastFetched(lastFetched: Long) {
        context.dataStore.edit {
            it[longPreferencesKey( LAST_FETCHED)] = lastFetched
        }
    }

    private fun saveTickers() {
        storage.saveTickers(tickerSet)
    }

    private suspend fun scheduleUpdate() {
        scheduleUpdateWithMs(alarmScheduler.msToNextAlarm(lastFetched.firstOrNull() ?: 0L))
    }

    private suspend fun scheduleUpdateWithMs(
        msToNextAlarm: Long
    ) {
        val updateTime = alarmScheduler.scheduleUpdate(msToNextAlarm, context)
        context.dataStore.edit {
            it[longPreferencesKey(NEXT_FETCH)] = updateTime.toInstant().toEpochMilli()
        }
        appPreferences.setRefreshing(false)
        widgetDataProvider.broadcastUpdateAllWidgets()
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

    fun hasTicker(ticker: String): Boolean {
        return tickerSet.contains(ticker)
    }

    suspend fun fetch(allowScheduling: Boolean = true): FetchResult<List<Quote>> = withContext(Dispatchers.IO) {
        if (tickerSet.isEmpty()) {
            Timber.d("No tickers/symbols to fetch")
            FetchResult.success(emptyList())
        } else {
            return@withContext try {
                if (allowScheduling) {
                    appPreferences.setRefreshing(true)
                }
                widgetDataProvider.broadcastUpdateAllWidgets()
                val fr = api.getStocks(tickerSet.toList())
                if (fr.hasError) {
                    throw fr.error
                }
                val fetchedStocks = fr.data
                if (fetchedStocks.isEmpty()) {
                    return@withContext FetchResult.failure<List<Quote>>(FetchException("Refresh failed"))
                } else {
                    synchronized(tickerSet) {
                        tickerSet.addAll(fetchedStocks.map { it.symbol })
                    }
                    _tickers.emit(tickerSet.toList())
                    // clean up existing tickers
                    tickerSet.toSet().forEach { ticker ->
                        if (!widgetDataProvider.containsTicker(ticker)) {
                            removeStock(ticker)
                        }
                    }
                    storage.saveQuotes(fetchedStocks)
                    fetchLocal()
                    if (allowScheduling) {
                        val lastFetched = clock.currentTimeMillis()
                        saveLastFetched(lastFetched)
                        scheduleUpdate()
                    }
                    widgetDataProvider.refreshWidgetDataList()
                    FetchResult.success(quoteMap.values.filter { tickerSet.contains(it.symbol) }.toList())
                }
            } catch (ex: CancellationException) {
                FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex))
            } catch (ex: Throwable) {
                Timber.w(ex)
                FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex))
            } finally {
                appPreferences.setRefreshing(false)
            }
        }
    }

    fun schedule() {
        coroutineScope.launch {
            scheduleUpdate()
            alarmScheduler.enqueuePeriodicRefresh()
        }
    }

    fun addStock(ticker: String): Collection<String> {
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

    fun hasPositions(): Boolean = quoteMap.filter { it.value.hasPositions() }.isNotEmpty()

    fun hasPosition(ticker: String): Boolean = quoteMap[ticker]?.hasPositions() ?: false

    fun getPosition(ticker: String): Position? = quoteMap[ticker]?.position

    suspend fun addHolding(
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

    suspend fun removePosition(
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

    fun addStocks(symbols: Collection<String>): Collection<String> {
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

    suspend fun removeStock(ticker: String): Collection<String> {
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

    suspend fun removeStocks(symbols: Collection<String>) {
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

    suspend fun fetchStock(ticker: String, allowCache: Boolean = true): FetchResult<Quote> {
        return fetchStockInternal(ticker, allowCache)
    }

    fun getStock(ticker: String): Quote? = quoteMap[ticker]

    fun addPortfolio(portfolio: List<Quote>) {
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
