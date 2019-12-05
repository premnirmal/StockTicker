package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.minutesInMs
import com.github.premnirmal.ticker.concurrency.ApplicationScope
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
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

  @Inject internal lateinit var api: StocksApi
  @Inject internal lateinit var context: Context
  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var bus: AsyncBus
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var alarmScheduler: AlarmScheduler
  @Inject internal lateinit var clock: AppClock
  @Inject internal lateinit var storage: StocksStorage

  private val tickerList: MutableList<String> = ArrayList()
  private val quoteList: MutableMap<String, Quote> = HashMap()
  private val positionList: MutableMap<String, Position> = HashMap()

  private var lastFetched: Long = 0L
  private var nextFetch: Long = 0L

  private val exponentialBackoff: ExponentialBackoff

  init {
    Injector.appComponent.inject(this)
    exponentialBackoff = ExponentialBackoff()
    val tickers = HashSet(storage.readTickers())
    tickerList.addAll(tickers)
    if (tickerList.isEmpty()) {
      tickerList.addAll(DEFAULT_STOCKS)
    }
    lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    nextFetch = preferences.getLong(NEXT_FETCH, 0L)
    if (lastFetched == 0L) {
      ApplicationScope.launch {
        fetch()
      }
    } else {
      fetchLocal()
    }
  }

  private fun fetchLocal() {
    try {
      synchronized(quoteList) {
        quoteList.clear()
        val quotes = storage.readStocks()
        for (quote in quotes) {
          quoteList[quote.symbol] = quote
        }
        positionList.clear()
        val positions = storage.readPositions()
        for (position in positions) {
          if (position.holdings.isNotEmpty()) {
            positionList[position.symbol] = position
          }
        }
        save()
      }
    } catch (e: Exception) {
      Timber.w(e)
    }
  }

  private fun save() {
    synchronized(quoteList) {
      preferences.edit()
          .putLong(LAST_FETCHED, lastFetched)
          .apply()
      storage.saveTickers(tickerList)
      storage.saveStocks(quoteList.values.toList())
      storage.savePositions(positionList.values.toList())
    }
  }

  private fun retryWithBackoff() {
    val backOffTimeMs = exponentialBackoff.getBackoffDurationMs()
    scheduleUpdateWithMs(backOffTimeMs)
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

  /////////////////////
  // public api
  /////////////////////

  override fun hasTicker(ticker: String): Boolean {
    synchronized(tickerList) {
      return tickerList.contains(ticker)
    }
  }

  override suspend fun fetch(): FetchResult<List<Quote>> = withContext(Dispatchers.IO) {
    if (tickerList.isEmpty()) {
      bus.send(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
      return@withContext FetchResult<List<Quote>>(
          _error = FetchException("No symbols in portfolio")
      )
    } else {
      val result = try {
        appPreferences.setRefreshing(true)
        widgetDataProvider.broadcastUpdateAllWidgets()
        val fetchedStocks = api.getStocks(tickerList)
        if (fetchedStocks.isEmpty()) {
          bus.send(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
          FetchResult<List<Quote>>(_error = FetchException("No symbols in portfolio"))
        }
        synchronized(quoteList) {
          tickerList.clear()
          fetchedStocks.mapTo(tickerList) { it.symbol }
          quoteList.clear()
          for (stock in fetchedStocks) {
            stock.position = getPosition(stock.symbol)
            quoteList[stock.symbol] = stock
          }
          lastFetched = api.lastFetched
          save()
          FetchResult(_data = fetchedStocks)
        }
      } catch (ex: Exception) {
        Timber.w(ex)
        appPreferences.setRefreshing(false)
        withContext(Dispatchers.Main) {
          InAppMessage.showToast(context, R.string.refresh_failed)
        }
        retryWithBackoff()
        FetchResult<List<Quote>>(_error = FetchException("Failed to fetch", ex))
      }
      appPreferences.setRefreshing(false)
      exponentialBackoff.reset()
      scheduleUpdate(true)
      return@withContext result
    }
  }

  override fun schedule() {
    scheduleUpdate()
  }

  override fun scheduleSoon() {
    scheduleUpdateWithMs(5L.minutesInMs(), true)
  }

  override fun addStock(ticker: String): Collection<String> {
    synchronized(quoteList) {
      if (!tickerList.contains(ticker)) {
        tickerList.add(ticker)
        val quote = Quote()
        quote.symbol = ticker
        quoteList[ticker] = quote
        save()
        bus.send(RefreshEvent())
        ApplicationScope.launch {
          fetch()
        }
      }
    }
    return tickerList
  }

  override fun hasPosition(ticker: String): Boolean = positionList.contains(ticker)

  override fun getPosition(ticker: String): Position? = positionList[ticker]

  override fun addPosition(
    ticker: String,
    shares: Float,
    price: Float
  ): Holding {
    synchronized(quoteList) {
      val quote = quoteList[ticker]
      var position = getPosition(ticker)
      if (position == null) {
        position = Position(ticker, ArrayList())
        positionList[ticker] = position
      }
      if (!tickerList.contains(ticker)) {
        tickerList.add(ticker)
      }
      val holding = Holding(shares, price)
      position.add(holding)
      quote?.position = position
      save()
      return holding
    }
  }

  override fun removePosition(
    ticker: String,
    holding: Holding
  ) {
    synchronized(positionList) {
      val position = getPosition(ticker)
      val quote = quoteList[ticker]
      position?.remove(holding)
      quote?.position = position
      save()
    }
  }

  override fun addStocks(tickers: Collection<String>): Collection<String> {
    synchronized(tickerList) {
      val filterNot = tickers.filterNot { tickerList.contains(it) }
      filterNot.forEach { tickerList.add(it) }
      save()
      if (filterNot.isNotEmpty()) {
        ApplicationScope.launch {
          fetch()
        }
      }
    }
    return tickerList
  }

  override fun removeStock(ticker: String): Collection<String> {
    synchronized(quoteList) {
      tickerList.remove(ticker)
      quoteList.remove(ticker)
      positionList.remove(ticker)
      save()
      scheduleUpdate(true)
      return tickerList
    }
  }

  override fun removeStocks(tickers: Collection<String>) {
    synchronized(quoteList) {
      tickers.forEach {
        tickerList.remove(it)
        quoteList.remove(it)
        positionList.remove(it)
      }
      save()
      scheduleUpdate(true)
    }
  }

  override suspend fun fetchStock(ticker: String): Quote? = withContext(Dispatchers.IO) {
    return@withContext quoteList[ticker] ?: run {
      api.getStock(ticker)
    }
  }

  override fun getStock(ticker: String): Quote? = quoteList[ticker]

  override fun getTickers(): List<String> = ArrayList(tickerList)

  override fun getPortfolio(): List<Quote> = quoteList.map { it.value }

  override fun addPortfolio(portfolio: List<Quote>) {
    synchronized(quoteList) {
      portfolio.forEach {
        val symbol = it.symbol
        if (!tickerList.contains(symbol)) tickerList.add(symbol)
        quoteList[symbol] = it
        if (it.hasPositions()) {
          positionList[symbol] = it.position!!
        }
      }
      save()
      widgetDataProvider.updateWidgets(tickerList)
      ApplicationScope.launch {
        fetch()
      }
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
