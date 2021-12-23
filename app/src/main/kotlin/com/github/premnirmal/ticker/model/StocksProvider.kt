package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.CoroutineContext

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider : IStocksProvider, CoroutineScope {

  companion object {

    private const val LAST_FETCHED = "LAST_FETCHED"
    private const val NEXT_FETCH = "NEXT_FETCH"
    private val DEFAULT_STOCKS = arrayOf("^GSPC", "^DJI", "GOOG", "AAPL", "MSFT")
  }

  @Inject lateinit var api: StocksApi
  @Inject lateinit var context: Context
  @Inject lateinit var preferences: SharedPreferences
  @Inject lateinit var appPreferences: AppPreferences
  @Inject lateinit var widgetDataProvider: WidgetDataProvider
  @Inject lateinit var alarmScheduler: AlarmScheduler
  @Inject lateinit var clock: AppClock
  @Inject lateinit var storage: StocksStorage

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main
  private val exponentialBackoff: ExponentialBackoff

  private val tickerSet: MutableSet<String> = HashSet()
  private val quoteMap: MutableMap<String, Quote> = HashMap()
  private val _fetchState = MutableStateFlow<FetchState>(FetchState.NotFetched)
  private val _nextFetch = MutableStateFlow<Long>(0)
  private val _lastFetched = MutableStateFlow<Long>(0)
  private val _tickers = MutableStateFlow<List<String>>(emptyList())
  private val _portfolio = MutableStateFlow<List<Quote>>(emptyList())

  init {
    Injector.appComponent.inject(this)
    exponentialBackoff = ExponentialBackoff()
    val tickers = storage.readTickers()
    this.tickerSet.addAll(tickers)
    if (this.tickerSet.isEmpty()) {
      this.tickerSet.addAll(DEFAULT_STOCKS)
    }
    val lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    _lastFetched.value = lastFetched
    val nextFetch = preferences.getLong(NEXT_FETCH, 0L)
    _nextFetch.value = nextFetch
    alarmScheduler.enqueuePeriodicRefresh(context)
    if (lastFetched == 0L) {
      launch {
        fetch()
      }
    } else {
      _fetchState.value = FetchState.Success(lastFetched)
      runBlocking { fetchLocal() }
    }
  }

  private suspend fun fetchLocal() = withContext(Dispatchers.IO) {
      try {
        val quotes = storage.readQuotes()
        synchronized(quoteMap) {
          quotes.forEach { quoteMap[it.symbol] = it }
        }
      } catch (e: Exception) {
        Timber.w(e)
      }
  }

  private fun saveLastFetched() {
    preferences.edit()
        .putLong(LAST_FETCHED, _lastFetched.value)
        .apply()
  }

  private fun saveTickers() {
    storage.saveTickers(tickerSet)
  }

  private fun scheduleUpdate() {
    scheduleUpdateWithMs(alarmScheduler.msToNextAlarm(_lastFetched.value))
  }

  private fun scheduleUpdateWithMs(
    msToNextAlarm: Long
  ) {
    val updateTime = alarmScheduler.scheduleUpdate(msToNextAlarm, context)
    _nextFetch.value = updateTime.toInstant().toEpochMilli()
    preferences.edit()
        .putLong(NEXT_FETCH, _nextFetch.value)
        .apply()
    appPreferences.setRefreshing(false)
    widgetDataProvider.broadcastUpdateAllWidgets()
  }

  private fun fetchStockInternal(ticker: String, allowCache: Boolean): Flow<FetchResult<Quote>> =
    flow {
      val quote = if (allowCache) quoteMap[ticker] else null
      quote?.let { FetchResult.success(quote) } ?: run {
        try {
          emit(api.getStock(ticker))
        } catch (ex: Exception) {
          Timber.w(ex)
          emit(FetchResult.failure<Quote>(FetchException("Failed to fetch", ex)))
        }
      }
    }.flowOn(Dispatchers.IO)

  /////////////////////
  // public api
  /////////////////////

  override fun hasTicker(ticker: String): Boolean {
    synchronized(tickerSet) {
      return tickerSet.contains(ticker)
    }
  }

  override fun fetch(): Flow<FetchResult<List<Quote>>> = flow {
    if (tickerSet.isEmpty()) {
      _fetchState.emit(FetchState.Failure(FetchException("No symbols in portfolio")))
      emit(FetchResult.failure<List<Quote>>(FetchException("No symbols in portfolio")))
    } else {
      try {
        appPreferences.setRefreshing(true)
        widgetDataProvider.broadcastUpdateAllWidgets()
        val fr = api.getStocks(tickerSet.toList())
        if (fr.hasError) {
          throw fr.error
        }
        val fetchedStocks = fr.data
        if (fetchedStocks.isEmpty()) {
          emit(FetchResult.failure<List<Quote>>(FetchException("Refresh failed")))
        } else {
          synchronized(tickers) {
            tickerSet.addAll(fetchedStocks.map { it.symbol })
          }
          storage.saveQuotes(fetchedStocks)
          fetchLocal()
          _lastFetched.emit(api.lastFetched)
          _fetchState.emit(FetchState.Success(api.lastFetched))
          saveLastFetched()
          exponentialBackoff.reset()
          scheduleUpdate()
          emit(FetchResult.success(fetchedStocks))
        }
      } catch (ex: Throwable) {
        Timber.w(ex)
        _fetchState.emit(FetchState.Failure(ex))
        val backOffTimeMs = exponentialBackoff.getBackoffDurationMs()
        scheduleUpdateWithMs(backOffTimeMs)
        emit(FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex)))
      } finally {
        appPreferences.setRefreshing(false)
      }
    }
  }.flowOn(Dispatchers.IO)

  override fun schedule() {
    scheduleUpdate()
    alarmScheduler.enqueuePeriodicRefresh(context, force = true)
  }

  override fun addStock(ticker: String): Collection<String> {
    synchronized(quoteMap) {
      if (!tickerSet.contains(ticker)) {
        tickerSet.add(ticker)
        val quote = Quote()
        quote.symbol = ticker
        quoteMap[ticker] = quote
        saveTickers()
        _tickers.value = tickerSet.toList()
        _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
        launch {
          fetchStockInternal(ticker, false).collect { result ->
            if (result.wasSuccessful) {
              val data = result.data
              quoteMap[ticker] = data
              storage.saveQuote(result.data)
            }
          }
        }
      }
    }
    return tickerSet
  }

  override fun hasPositions(): Boolean = quoteMap.filter { it.value.hasPositions() }.isNotEmpty()

  override fun hasPosition(ticker: String): Boolean = quoteMap[ticker]?.hasPositions() ?: false

  override fun getPosition(ticker: String): Position? = quoteMap[ticker]?.position

  override fun addHolding(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding {
    synchronized(quoteMap) {
      val quote = quoteMap[ticker]
      var position = getPosition(ticker)
      if (position == null) {
        position = Position(ticker)
      }
      if (!tickerSet.contains(ticker)) {
        tickerSet.add(ticker)
        _tickers.value = tickerSet.toList()
        saveTickers()
      }
      val holding = Holding(ticker, shares, price)
      position.add(holding)
      quote?.position = position
      launch {
        val id = storage.addHolding(holding)
        holding.id = id
      }
      _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
      return holding
    }
  }

  override fun removePosition(
    ticker: String,
    holding: Holding
  ) {
    synchronized(quoteMap) {
      val position = getPosition(ticker)
      val quote = quoteMap[ticker]
      position?.remove(holding)
      quote?.position = position
      launch {
        storage.removeHolding(ticker, holding)
      }
      _tickers.value = tickerSet.toList()
      _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
    }
  }

  override fun addStocks(symbols: Collection<String>): Collection<String> {
    synchronized(this.tickerSet) {
      val filterNot = symbols.filterNot { this.tickerSet.contains(it) }
      filterNot.forEach { this.tickerSet.add(it) }
      saveTickers()
      if (filterNot.isNotEmpty()) {
        launch {
          fetch()
        }
      }
      _tickers.value = tickerSet.toList()
      _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
    }
    return this.tickerSet
  }

  override fun removeStock(ticker: String): Collection<String> {
    synchronized(quoteMap) {
      tickerSet.remove(ticker)
      saveTickers()
      quoteMap.remove(ticker)
      _tickers.value = tickerSet.toList()
      _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
    }
    launch {
      storage.removeQuoteBySymbol(ticker)
    }
    return tickerSet
  }

  override fun removeStocks(symbols: Collection<String>) {
    synchronized(quoteMap) {
      symbols.forEach {
        tickerSet.remove(it)
        quoteMap.remove(it)
      }
      _tickers.value = tickerSet.toList()
      _portfolio.value = quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }
    }
    saveTickers()
    launch {
      storage.removeQuotesBySymbol(symbols.toList())
    }
  }

  override fun fetchStock(ticker: String): Flow<FetchResult<Quote>> {
    return fetchStockInternal(ticker, true)
  }

  override fun getStock(ticker: String): Quote? = quoteMap[ticker]

  override val tickers: StateFlow<List<String>>
    get() = _tickers

  override val portfolio: StateFlow<List<Quote>>
    get() = _portfolio//quoteMap.filter { widgetDataProvider.containsTicker(it.key) }.map { it.value }

  override fun addPortfolio(portfolio: List<Quote>) {
    synchronized(quoteMap) {
      portfolio.forEach {
        val symbol = it.symbol
        if (!tickerSet.contains(symbol)) tickerSet.add(symbol)
        quoteMap[symbol] = it
      }
      saveTickers()
      widgetDataProvider.updateWidgets(tickerSet.toList())
    }
    launch {
      storage.saveQuotes(portfolio)
      fetchLocal()
      fetch()
    }
  }

  override val fetchState: StateFlow<FetchState>
    get() = _fetchState

  override val nextFetchMs: StateFlow<Long>
    get() = _nextFetch
}
