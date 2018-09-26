package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.components.minutesInMs
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.ticker.events.RefreshEvent
import com.github.premnirmal.ticker.network.RobindahoodException
import com.github.premnirmal.ticker.network.SimpleSubscriber
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.ticker.network.data.Position
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.exceptions.CompositeException
import io.reactivex.schedulers.Schedulers
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
class StocksProvider @Inject constructor() : IStocksProvider {

  companion object {

    private const val LAST_FETCHED = "LAST_FETCHED"
    private const val NEXT_FETCH = "NEXT_FETCH"
    private val DEFAULT_STOCKS = arrayOf("^GSPC", "^DJI", "GOOG", "AAPL", "MSFT")
    private const val HAS_MIGRATED_POSITIONS = "has_migrated_positions"
  }

  @Inject internal lateinit var api: StocksApi
  @Inject internal lateinit var context: Context
  @Inject internal lateinit var preferences: SharedPreferences
  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var bus: RxBus
  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var alarmScheduler: AlarmScheduler
  @Inject internal lateinit var clock: AppClock
  @Inject internal lateinit var mainThreadHandler: Handler
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
    tickerList.addAll(storage.readTickers())
    if (tickerList.isEmpty()) {
      tickerList.addAll(DEFAULT_STOCKS)
    }
    lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    nextFetch = preferences.getLong(NEXT_FETCH, 0L)
    if (lastFetched == 0L) {
      fetch().subscribe(SimpleSubscriber())
    } else {
      fetchLocal()
    }
  }

  private fun fetchLocal() {
    synchronized(quoteList) {
      quoteList.clear()
      val quotes = storage.readStocks()
      for (quote in quotes) {
        quoteList[quote.symbol] = quote
      }
      positionList.clear()
      val hasMigratedPositions = preferences.getBoolean(HAS_MIGRATED_POSITIONS, false)
      if (!hasMigratedPositions) {
        val oldPositions = storage.readPositionsLegacy()
        val newPositions = ArrayList<Position>()
        for (quote in oldPositions) {
          val position =
            Position(quote.symbol, arrayListOf(Holding(quote.positionShares, quote.positionPrice)))
          newPositions.add(position)
          quote.position = position
        }
        storage.savePositions(newPositions)
        preferences.edit().putBoolean(HAS_MIGRATED_POSITIONS, true).apply()
      }
      val positions = storage.readPositionsNew()
      for (position in positions) {
        if (!position.holdings.isEmpty()) {
          positionList[position.symbol] = position
        }
      }
      save()
    }
  }

  private fun save() {
    synchronized(quoteList) {
      preferences.edit().putLong(LAST_FETCHED, lastFetched).apply()
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

  private fun scheduleUpdateWithMs(msToNextAlarm: Long, refresh: Boolean = false) {
    val updateTime = alarmScheduler.scheduleUpdate(msToNextAlarm, context)
    nextFetch = updateTime.toInstant().toEpochMilli()
    preferences.edit().putLong(NEXT_FETCH, nextFetch).apply()
    appPreferences.setRefreshing(false)
    widgetDataProvider.broadcastUpdateAllWidgets()
    if (refresh) {
      bus.post(RefreshEvent())
    }
  }

  private fun ZonedDateTime.createTimeString(): String {
    val fetched: String
    val fetchedDayOfWeek = dayOfWeek.value
    val today = clock.todayZoned().dayOfWeek.value
    fetched = if (today == fetchedDayOfWeek) {
      AppPreferences.TIME_FORMATTER.format(this)
    } else {
      val day: String = DayOfWeek.from(this).getDisplayName(SHORT, Locale.getDefault())
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

  override fun fetch(): Observable<List<Quote>> {
    if (tickerList.isEmpty()) {
      bus.post(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
      return Observable.error<List<Quote>>(Exception("No symbols in portfolio"))
          .subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
    } else {
      return api.getStocks(tickerList).doOnSubscribe {
        appPreferences.setRefreshing(true)
        widgetDataProvider.broadcastUpdateAllWidgets()
      }.doOnNext { stocks ->
        if (stocks.isEmpty()) {
          bus.post(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
          throw IllegalStateException("No symbols in portfolio")
        } else {
          synchronized(quoteList) {
            tickerList.clear()
            stocks.mapTo(tickerList) { it.symbol }
            quoteList.clear()
            for (stock in stocks) {
              stock.position = getPosition(stock.symbol)
              quoteList[stock.symbol] = stock
            }
            lastFetched = api.lastFetched
            save()
          }
        }
      }.doOnError { t ->
        var errorPosted = false
        appPreferences.setRefreshing(false)
        if (t is CompositeException) {
          for (exception in t.exceptions) {
            if (exception is RobindahoodException) {
              exception.message?.let {
                if (!bus.post(ErrorEvent(it))) {
                  mainThreadHandler.post {
                    InAppMessage.showToast(context, it)
                  }
                  errorPosted = true
                }
              }
              break
            }
          }
        } else {
          Timber.e(t)
        }
        if (!errorPosted) {
          mainThreadHandler.post {
            InAppMessage.showToast(context, R.string.refresh_failed)
          }
        }
        retryWithBackoff()
      }.doOnComplete {
        appPreferences.setRefreshing(false)
        exponentialBackoff.reset()
        scheduleUpdate(true)
      }.subscribeOn(Schedulers.io()).observeOn(AndroidSchedulers.mainThread())
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
        fetch().subscribe(SimpleSubscriber())
      }
    }
    return tickerList
  }

  override fun hasPosition(ticker: String): Boolean = positionList.contains(ticker)

  override fun getPosition(ticker: String): Position? = positionList[ticker]

  override fun addPosition(ticker: String, shares: Float, price: Float): Holding {
    synchronized(quoteList) {
      val quote = getStock(ticker)
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

  override fun removePosition(ticker: String, holding: Holding) {
    synchronized(positionList) {
      val position = getPosition(ticker)
      val quote = getStock(ticker)
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
        fetch().subscribe(SimpleSubscriber())
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
      scheduleUpdate()
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
      scheduleUpdate()
    }
  }

  override fun getStock(ticker: String): Quote? = quoteList[ticker]

  override fun getTickers(): List<String> = ArrayList(tickerList)

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
