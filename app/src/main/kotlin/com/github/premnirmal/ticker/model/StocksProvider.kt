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
import kotlinx.coroutines.CancellationException
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

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider : IStocksProvider {

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
  @Inject lateinit var coroutineScope: CoroutineScope

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
    _tickers.tryEmit(tickerSet.toList())
    val lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    _lastFetched.tryEmit(lastFetched)
    val nextFetch = preferences.getLong(NEXT_FETCH, 0L)
    _nextFetch.tryEmit(nextFetch)
    coroutineScope.launch {
      alarmScheduler.enqueuePeriodicRefresh(context)
    }
    runBlocking { fetchLocal() }
    if (lastFetched == 0L) {
      coroutineScope.launch {
        fetch().collect()
      }
    } else {
      _fetchState.tryEmit(FetchState.Success(lastFetched))
    }
  }

  private suspend fun fetchLocal() = withContext(Dispatchers.IO) {
      try {
        val quotes = storage.readQuotes()
        synchronized(quoteMap) {
          quotes.forEach { quoteMap[it.symbol] = it }
        }
        _portfolio.emit(quoteMap.values.toList())
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
    _nextFetch.tryEmit(updateTime.toInstant().toEpochMilli())
    preferences.edit()
        .putLong(NEXT_FETCH, _nextFetch.value)
        .apply()
    appPreferences.setRefreshing(false)
    widgetDataProvider.broadcastUpdateAllWidgets()
  }

  private fun fetchStockInternal(ticker: String, allowCache: Boolean): Flow<FetchResult<Quote>> =
    flow {
      val quote = if (allowCache) quoteMap[ticker] else null
      quote?.let { emit(FetchResult.success(quote)) } ?: run {
        try {
          emit(api.getStock(ticker))
        } catch (ex: CancellationException) {
          // ignore
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
    return tickerSet.contains(ticker)
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
          synchronized(tickerSet) {
            tickerSet.addAll(fetchedStocks.map { it.symbol })
          }
          _tickers.emit(tickerSet.toList())
          // clean up existing tickers
          ArrayList(tickerSet).forEach { ticker ->
            if (!widgetDataProvider.containsTicker(ticker)) {
              removeStock(ticker)
            }
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
      } catch(ex: CancellationException) {
        val backOffTimeMs = exponentialBackoff.getBackoffDurationMs()
        scheduleUpdateWithMs(backOffTimeMs)
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
    coroutineScope.launch {
      alarmScheduler.enqueuePeriodicRefresh(context, force = true)
    }
  }

  override fun addStock(ticker: String): Collection<String> {
    synchronized(quoteMap) {
      if (!tickerSet.contains(ticker)) {
        tickerSet.add(ticker)
        val quote = Quote()
        quote.symbol = ticker
        quoteMap[ticker] = quote
        saveTickers()
      }
    }
    _tickers.tryEmit(tickerSet.toList())
    _portfolio.tryEmit(quoteMap.values.toList())
    coroutineScope.launch {
      fetchStockInternal(ticker, false).collect { result ->
        if (result.wasSuccessful) {
          val data = result.data
          quoteMap[ticker] = data
          storage.saveQuote(result.data)
          _portfolio.tryEmit(quoteMap.values.toList())
        }
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
    _portfolio.emit(quoteMap.values.toList())
    return holding
  }

  override suspend fun removePosition(
    ticker: String,
    holding: Holding
  ) {
    synchronized(quoteMap) {
      val position = getPosition(ticker)
      val quote = quoteMap[ticker]
      position?.remove(holding)
      quote?.position = position
    }
    storage.removeHolding(ticker, holding)
    _portfolio.emit(quoteMap.values.toList())
  }

  override fun addStocks(symbols: Collection<String>): Collection<String> {
    synchronized(this.tickerSet) {
      val filterNot = symbols.filterNot { this.tickerSet.contains(it) }
      filterNot.forEach { this.tickerSet.add(it) }
      saveTickers()
      if (filterNot.isNotEmpty()) {
        coroutineScope.launch {
          fetch().collect()
        }
      }
    }
    _tickers.tryEmit(tickerSet.toList())
    _portfolio.tryEmit(quoteMap.values.toList())
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
    _portfolio.emit(quoteMap.values.toList())
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
    _portfolio.emit(quoteMap.values.toList())
    saveTickers()
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
    }
    saveTickers()
    widgetDataProvider.updateWidgets(tickerSet.toList())
    coroutineScope.launch {
      storage.saveQuotes(portfolio)
      fetchLocal()
      fetch().collect()
    }
  }

  override val fetchState: StateFlow<FetchState>
    get() = _fetchState

  override val nextFetchMs: StateFlow<Long>
    get() = _nextFetch
}
