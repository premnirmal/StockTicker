package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.AppPreferences.Companion.toCommaSeparatedString
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
import java.util.Arrays
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider @Inject constructor() : IStocksProvider {

  companion object {

    private fun List<Quote>.positionsToString(): String {
      val builder = StringBuilder()
      for (stock in this) {
        if (stock.isPosition) {
          builder.append(stock.symbol)
          builder.append(",")
          builder.append(stock.isPosition)
          builder.append(",")
          builder.append(stock.positionPrice)
          builder.append(",")
          builder.append(stock.positionShares)
          builder.append("\n")
        }
      }
      return builder.toString()
    }

    private fun String.stringToPositions(): MutableList<Quote> {
      val tickerListCSV = ArrayList(Arrays.asList(
          *this.split("\n".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
      val stockList = ArrayList<Quote>()
      var tickerFields: ArrayList<String>
      var tmpQuote: Quote
      for (tickerCSV in tickerListCSV) {
        tickerFields = ArrayList(Arrays.asList(
            *tickerCSV.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        if (tickerFields.size >= 4 && java.lang.Boolean.parseBoolean(tickerFields[1])) {
          tmpQuote = Quote()
          tmpQuote.isPosition = true
          tmpQuote.symbol = tickerFields[0]
          tmpQuote.positionPrice = java.lang.Float.parseFloat(tickerFields[2])
          tmpQuote.positionShares = java.lang.Float.parseFloat(tickerFields[3])
          stockList.add(tmpQuote)
        }
      }
      return stockList
    }

    private const val LAST_FETCHED = "LAST_FETCHED"
    private const val NEXT_FETCH = "NEXT_FETCH"
    private const val POSITION_LIST = "POSITION_LIST"
    private const val DEFAULT_STOCKS = "SPY,DIA,GOOG,AAPL,MSFT"
    private const val SORTED_STOCK_LIST = AppPreferences.SORTED_STOCK_LIST
  }

  @Inject
  internal lateinit var api: StocksApi
  @Inject
  internal lateinit var context: Context
  @Inject
  internal lateinit var preferences: SharedPreferences
  @Inject
  internal lateinit var appPreferences: AppPreferences
  @Inject
  internal lateinit var bus: RxBus
  @Inject
  internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject
  internal lateinit var alarmScheduler: AlarmScheduler
  @Inject
  internal lateinit var clock: AppClock
  @Inject
  internal lateinit var mainThreadHandler: Handler

  private val tickerList: MutableList<String>
  private val quoteList: MutableList<Quote> = ArrayList()
  private val positionList: MutableList<Quote>
  private var lastFetched: Long = 0L
  private var nextFetch: Long = 0L
  private val storage: StocksStorage
  private val exponentialBackoff: ExponentialBackoff
  private var backOffAttemptCount = 1

  init {
    Injector.appComponent.inject(this)
    storage = StocksStorage()
    exponentialBackoff = ExponentialBackoff()
    backOffAttemptCount = appPreferences.backOffAttemptCount()
    val tickerListVars = preferences.getString(SORTED_STOCK_LIST, DEFAULT_STOCKS)
    tickerList = ArrayList(Arrays.asList(
        *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))

    positionList = preferences.getString(POSITION_LIST, "").stringToPositions()

    val tickerList = this.tickerList.toCommaSeparatedString()
    preferences.edit().putString(SORTED_STOCK_LIST, tickerList).apply()
    lastFetched = try {
      preferences.getLong(LAST_FETCHED, 0L)
    } catch (e: Exception) {
      0L
    }
    nextFetch = preferences.getLong(NEXT_FETCH, 0)
    if (lastFetched == 0L) {
      fetch().subscribe(SimpleSubscriber())
    } else {
      fetchLocal()
    }
  }

  private fun fetchLocal() {
    synchronized(quoteList, {
      quoteList.clear()
      quoteList.addAll(storage.readStocks())
    })
  }

  private fun save() {
    synchronized(quoteList, {
      preferences.edit()
          .putString(POSITION_LIST, positionList.positionsToString())
          .putString(SORTED_STOCK_LIST, tickerList.toCommaSeparatedString())
          .putLong(LAST_FETCHED, lastFetched)
          .apply()
      storage.saveStocks(quoteList)
    })
  }

  private fun retryWithBackoff() {
    val backOffTimeMs = exponentialBackoff.getBackoffDurationMs(backOffAttemptCount++)
    saveBackOffAttemptCount()
    scheduleUpdateWithMs(backOffTimeMs)
  }

  private fun saveBackOffAttemptCount() {
    appPreferences.setBackOffAttemptCount(backOffAttemptCount)
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
    synchronized(tickerList, {
      return tickerList.contains(ticker)
    })
  }

  override fun fetch(): Observable<List<Quote>> {
    if (tickerList.isEmpty()) {
      bus.post(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
      return Observable.error<List<Quote>>(Exception("No symbols in portfolio"))
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
    } else {
      return api.getStocks(tickerList)
          .doOnSubscribe {
            appPreferences.setRefreshing(true)
            widgetDataProvider.broadcastUpdateAllWidgets()
          }
          .doOnNext { stocks ->
            if (stocks.isEmpty()) {
              bus.post(ErrorEvent(context.getString(R.string.no_symbols_in_portfolio)))
              throw RuntimeException("No symbols in portfolio")
            } else {
              synchronized(quoteList, {
                tickerList.clear()
                stocks.mapTo(tickerList) { it.symbol }
                quoteList.clear()
                for (stock in stocks) {
                  if (positionList.contains(stock)) {
                    val index = positionList.indexOf(stock)
                    stock.isPosition = positionList[index].isPosition
                    stock.positionPrice = positionList[index].positionPrice
                    stock.positionShares = positionList[index].positionShares
                  }
                  quoteList.add(stock)
                }
                lastFetched = api.lastFetched
                save()
              })
            }
          }
          .doOnError { t ->
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
          }
          .doOnComplete {
            appPreferences.setRefreshing(false)
            backOffAttemptCount = 1
            saveBackOffAttemptCount()
            scheduleUpdate(true)
          }
          .subscribeOn(Schedulers.io())
          .observeOn(AndroidSchedulers.mainThread())
    }
  }

  override fun schedule() {
    scheduleUpdate()
  }

  override fun scheduleSoon() {
    scheduleUpdateWithMs(5L.minutesInMs(), true)
  }

  override fun addStock(ticker: String): Collection<String> {
    synchronized(quoteList, {
      if (!tickerList.contains(ticker)) {
        tickerList.add(ticker)
        val quote = Quote()
        quote.symbol = ticker
        quoteList.add(quote)
        save()
        fetch().subscribe(SimpleSubscriber())
      }
    })
    return tickerList
  }

  override fun addPosition(ticker: String, shares: Float, price: Float) {
    synchronized(quoteList, {
      var position = getStock(ticker)
      if (position == null) {
        position = Quote()
        position.symbol = ticker
      }
      if (!tickerList.contains(ticker)) {
        tickerList.add(ticker)
      }
      if (shares > 0) {
        position.isPosition = true
        position.positionPrice = price
        position.positionShares = shares
        positionList.remove(position)
        positionList.add(position)
        quoteList.remove(position)
        quoteList.add(position)
        save()
      } else {
        removePosition(ticker)
      }
    })
  }

  override fun removePosition(ticker: String) {
    synchronized(positionList, {
      val position = getStock(ticker) ?: return
      position.isPosition = false
      position.positionPrice = 0f
      position.positionShares = 0f
      positionList.remove(position)
      save()
    })
  }

  override fun addStocks(tickers: Collection<String>): Collection<String> {
    synchronized(tickerList, {
      val filterNot = tickers
          .filterNot { tickerList.contains(it) }
      filterNot
          .forEach { tickerList.add(it) }
      save()
      if (filterNot.isNotEmpty()) {
        fetch().subscribe(SimpleSubscriber())
      }
    })
    return tickerList
  }

  override fun removeStock(ticker: String): Collection<String> {
    synchronized(quoteList, {
      val ticker2 = "^$ticker" // in case it was an index
      tickerList.remove(ticker)
      tickerList.remove(ticker2)
      val dummy = Quote()
      val dummy2 = Quote()
      dummy.symbol = ticker
      dummy2.symbol = ticker2
      quoteList.remove(dummy)
      quoteList.remove(dummy2)
      positionList.remove(dummy)
      positionList.remove(dummy2)
      save()
      scheduleUpdate()
      return tickerList
    })
  }

  override fun removeStocks(tickers: Collection<String>) {
    synchronized(quoteList, {
      tickers.forEach {
        val ticker2 = "^$it" // in case it was an index
        tickerList.remove(it)
        tickerList.remove(ticker2)
        val dummy = Quote()
        val dummy2 = Quote()
        dummy.symbol = it
        dummy2.symbol = ticker2
        quoteList.remove(dummy)
        quoteList.remove(dummy2)
        positionList.remove(dummy)
        positionList.remove(dummy2)
      }
      save()
      scheduleUpdate()
    })
  }

  override fun getStock(ticker: String): Quote? {
    synchronized(quoteList, {
      val dummy = Quote()
      dummy.symbol = ticker
      val index = quoteList.indexOf(dummy)
      return if (index >= 0) {
        val stock = quoteList[index]
        stock
      } else {
        null
      }
    })
  }

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
