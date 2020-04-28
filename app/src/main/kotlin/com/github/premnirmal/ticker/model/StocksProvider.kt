package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.minutesInMs
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.repo.StocksStorage
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.format.TextStyle.SHORT
import timber.log.Timber
import java.util.ArrayList
import java.util.Locale
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
  @Inject lateinit var bus: AsyncBus
  @Inject lateinit var widgetDataProvider: WidgetDataProvider
  @Inject lateinit var alarmScheduler: AlarmScheduler
  @Inject lateinit var clock: AppClock
  @Inject lateinit var storage: StocksStorage

  override val coroutineContext: CoroutineContext
    get() = Dispatchers.Main

  private val tickers: MutableSet<String> = HashSet()
  private val quoteMap: MutableMap<String, Quote> = HashMap()

  private var lastFetched: Long = 0L
  private var nextFetch: Long = 0L

  private val exponentialBackoff: ExponentialBackoff

  init {
    Injector.appComponent.inject(this)
    exponentialBackoff = ExponentialBackoff()
    val tickers = storage.readTickers()
    this.tickers.addAll(tickers)
    if (this.tickers.isEmpty()) {
      this.tickers.addAll(DEFAULT_STOCKS)
    }
    lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    nextFetch = preferences.getLong(NEXT_FETCH, 0L)
    if (lastFetched == 0L) {
      launch {
        fetch()
      }
    } else {
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
        .putLong(LAST_FETCHED, lastFetched)
        .apply()
  }

  private fun saveTickers() {
    storage.saveTickers(tickers)
  }

  private fun scheduleUpdate(refresh: Boolean = false) {
    scheduleUpdateWithMs(msToNextAlarm, refresh)
  }

  private val msToNextAlarm: Long
    get() = alarmScheduler.msToNextAlarm(lastFetched)

  private fun scheduleUpdateWithMs(
    msToNextAlarm: Long,
    refresh: Boolean = false
  ) {
    val updateTime = alarmScheduler.scheduleUpdate(msToNextAlarm, context)
    nextFetch = updateTime.toInstant()
        .toEpochMilli()
    preferences.edit()
        .putLong(NEXT_FETCH, nextFetch)
        .apply()
    appPreferences.setRefreshing(false)
    widgetDataProvider.broadcastUpdateAllWidgets()
    if (refresh) {
      bus.send(RefreshEvent())
    }
  }

  private fun ZonedDateTime.createTimeString(): String {
    val fetched: String
    val fetchedDayOfWeek = dayOfWeek.value
    val today = clock.todayZoned()
        .dayOfWeek.value
    fetched = if (today == fetchedDayOfWeek) {
      AppPreferences.TIME_FORMATTER.format(this)
    } else {
      val day: String = DayOfWeek.from(this)
          .getDisplayName(SHORT, Locale.getDefault())
      val timeStr: String = AppPreferences.TIME_FORMATTER.format(this)
      "$timeStr $day"
    }
    return fetched
  }

  private suspend fun fetchStockInternal(ticker: String, allowCache: Boolean): FetchResult<Quote> = withContext(Dispatchers.IO) {
    val quote = if (allowCache) quoteMap[ticker] else null
    return@withContext quote?.let { FetchResult.success(quote) } ?: run {
      try {
        return@run api.getStock(ticker)
      } catch (ex: Exception) {
        Timber.w(ex)
        withContext(Dispatchers.Main) {
          InAppMessage.showToast(context, R.string.error_fetching_stock)
        }
        return@run FetchResult.failure<Quote>(FetchException("Failed to fetch", ex))
      }
    }
  }

  /////////////////////
  // public api
  /////////////////////

  override fun hasTicker(ticker: String): Boolean {
    synchronized(tickers) {
      return tickers.contains(ticker)
    }
  }

  override suspend fun fetch(): FetchResult<List<Quote>> = withContext(Dispatchers.IO) {
    if (tickers.isEmpty()) {
      bus.send(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
      return@withContext FetchResult.failure<List<Quote>>(FetchException("No symbols in portfolio"))
    } else {
      try {
        appPreferences.setRefreshing(true)
        widgetDataProvider.broadcastUpdateAllWidgets()
        val fr = api.getStocks(tickers.toList())
        val fetchedStocks = fr.data
        if (fetchedStocks.isEmpty()) {
          bus.send(ErrorEvent(context.getString(R.string.refresh_failed)))
          return@withContext FetchResult.failure<List<Quote>>(FetchException("Refresh failed"))
        } else {
          synchronized(tickers) {
            tickers.addAll(fetchedStocks.map { it.symbol })
          }
          storage.saveQuotes(fetchedStocks)
          fetchLocal()
          lastFetched = api.lastFetched
          saveLastFetched()
          exponentialBackoff.reset()
          scheduleUpdate(true)
          return@withContext FetchResult.success(fetchedStocks)
        }
      } catch (ex: Exception) {
        Timber.w(ex)
        if (!bus.send(ErrorEvent(context.getString(R.string.refresh_failed)))) {
          withContext(Dispatchers.Main) {
            InAppMessage.showToast(context, R.string.refresh_failed)
          }
        }
        val backOffTimeMs = exponentialBackoff.getBackoffDurationMs()
        scheduleUpdateWithMs(backOffTimeMs)
        return@withContext FetchResult.failure<List<Quote>>(FetchException("Failed to fetch", ex))
      } finally {
        appPreferences.setRefreshing(false)
      }
    }
  }

  override fun schedule() {
    scheduleUpdate()
  }

  override fun scheduleSoon() {
    scheduleUpdateWithMs(5L.minutesInMs(), true)
  }

  override fun addStock(ticker: String): Collection<String> {
    synchronized(quoteMap) {
      if (!tickers.contains(ticker)) {
        tickers.add(ticker)
        val quote = Quote()
        quote.symbol = ticker
        quoteMap[ticker] = quote
        saveTickers()
        bus.send(RefreshEvent())
        launch {
          val result = fetchStockInternal(ticker, false)
          if (result.wasSuccessful) {
            val data = result.data
            quoteMap[ticker] = data
            storage.saveQuote(result.data)
            bus.send(RefreshEvent())
          }
        }
      }
    }
    return tickers
  }

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
      if (!tickers.contains(ticker)) {
        tickers.add(ticker)
        saveTickers()
      }
      val holding = Holding(ticker, shares, price)
      position.add(holding)
      quote?.position = position
      launch {
        val id = storage.addHolding(holding)
        holding.id = id
      }
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
    }
  }

  override fun addStocks(symbols: Collection<String>): Collection<String> {
    synchronized(this.tickers) {
      val filterNot = symbols.filterNot { this.tickers.contains(it) }
      filterNot.forEach { this.tickers.add(it) }
      saveTickers()
      if (filterNot.isNotEmpty()) {
        launch {
          fetch()
        }
      }
    }
    return this.tickers
  }

  override fun removeStock(ticker: String): Collection<String> {
    synchronized(quoteMap) {
      tickers.remove(ticker)
      saveTickers()
      quoteMap.remove(ticker)
    }
    scheduleUpdate(true)
    launch {
      storage.removeQuoteBySymbol(ticker)
    }
    return tickers
  }

  override fun removeStocks(symbols: Collection<String>) {
    synchronized(quoteMap) {
      symbols.forEach {
        tickers.remove(it)
        quoteMap.remove(it)
      }
    }
    saveTickers()
    launch {
      storage.removeQuotesBySymbol(symbols.toList())
    }
    scheduleUpdate(true)
  }

  override suspend fun fetchStock(ticker: String): FetchResult<Quote> {
    return fetchStockInternal(ticker, true)
  }

  override fun getStock(ticker: String): Quote? = quoteMap[ticker]

  override fun getTickers(): List<String> = ArrayList(tickers)

  override fun getPortfolio(): List<Quote> = quoteMap.map { it.value }

  override fun addPortfolio(portfolio: List<Quote>) {
    synchronized(quoteMap) {
      portfolio.forEach {
        val symbol = it.symbol
        if (!tickers.contains(symbol)) tickers.add(symbol)
        quoteMap[symbol] = it
      }
      saveTickers()
      widgetDataProvider.updateWidgets(tickers.toList())
    }
    launch {
      storage.saveQuotes(portfolio)
      fetchLocal()
      fetch()
    }
  }

  override fun lastFetched(): String {
    return if (lastFetched > 0L) {
      val instant = Instant.ofEpochMilli(lastFetched)
      val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
      time.createTimeString()
    } else {
      "--"
    }
  }

  override fun nextFetch(): String {
    return if (nextFetch > 0) {
      val instant = Instant.ofEpochMilli(nextFetch)
      val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
      time.createTimeString()
    } else {
      "--"
    }
  }

  override fun nextFetchMs(): Long {
    return nextFetch
  }
}
