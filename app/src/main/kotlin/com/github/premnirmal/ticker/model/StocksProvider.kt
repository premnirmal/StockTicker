package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.SimpleSubscriber
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.network.StocksApi
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import rx.Observable
import rx.android.schedulers.AndroidSchedulers
import rx.schedulers.Schedulers
import java.text.DateFormatSymbols
import java.util.ArrayList
import java.util.Arrays
import java.util.Collections
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider @Inject constructor() : IStocksProvider {

  companion object {

    internal val LAST_FETCHED = "LAST_FETCHED"
    internal val NEXT_FETCH = "NEXT_FETCH"
    internal val POSITION_LIST = "POSITION_LIST"
    internal val DEFAULT_STOCKS = "SPY,DIA,GOOG,AAPL,MSFT"

    val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
  }

  @Inject internal lateinit var api: StocksApi
  @Inject internal lateinit var context: Context
  @Inject internal lateinit var preferences: SharedPreferences

  internal val tickerList: MutableList<String>
  internal val stockList: MutableList<Stock> = ArrayList()
  internal val positionList: MutableList<Stock>
  internal var lastFetched: Long = 0L
  internal var nextFetch: Long = 0L
  internal val storage: StocksStorage

  init {
    Injector.inject(this)
    storage = StocksStorage(context)
    val tickerListVars = preferences.getString(SORTED_STOCK_LIST, DEFAULT_STOCKS)
    tickerList = ArrayList(Arrays.asList(
        *tickerListVars.split(",".toRegex()).dropLastWhile(String::isEmpty).toTypedArray()))

    positionList = Tools.stringToPositions(preferences.getString(POSITION_LIST, ""))

    val tickerList = Tools.toCommaSeparatedString(this.tickerList)
    preferences.edit().putString(SORTED_STOCK_LIST, tickerList).apply()
    try {
      lastFetched = preferences.getLong(LAST_FETCHED, 0L)
    } catch (e: Exception) {
      lastFetched = 0L
    }
    nextFetch = preferences.getLong(NEXT_FETCH, 0)
    if (lastFetched == 0L) {
      fetch().subscribe(SimpleSubscriber<List<Stock>>())
    } else {
      fetchLocal()
    }
  }

  internal fun fetchLocal() {
    synchronized(stockList, {
      stockList.clear()
      stockList.addAll(storage.readSynchronous())
      if (!stockList.isEmpty()) {
        sortStockList()
        sendBroadcast()
      } else {
        fetch().subscribe(SimpleSubscriber<List<Stock>>())
      }
    })
  }

  internal fun save() {
    preferences.edit().putString(POSITION_LIST, Tools.positionsToString(positionList))
        .putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList))
        .putLong(LAST_FETCHED, lastFetched)
        .apply()
    storage.save(stockList).subscribe(object : SimpleSubscriber<Boolean>() {
      override fun onError(e: Throwable) {
        e.printStackTrace()
      }

      override fun onNext(result: Boolean) {
        if (!result) {
          CrashLogger.logException(RuntimeException("Save failed"))
        }
      }
    })
  }

  override fun fetch(): Observable<List<Stock>> {
    return api.getStocks(tickerList)
        .doOnError { e ->
          // why does this happen?
          CrashLogger.logException(RuntimeException("Encountered onError when fetching stocks", e))
          scheduleUpdate(SystemClock.elapsedRealtime() + (60 * 1000)) // 1 minute
          AlarmScheduler.sendBroadcast(context)
        }
        .doOnNext { stocks ->
          synchronized(stockList, {
            stockList.clear()
            stockList.addAll(stocks)
            lastFetched = api.lastFetched
            save()
            sendBroadcast()
          })
        }
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
  }

  internal fun sendBroadcast() {
    scheduleUpdate(msToNextAlarm)
    AlarmScheduler.sendBroadcast(context)
  }

  internal val msToNextAlarm: Long
    get() = AlarmScheduler.msOfNextAlarm()

  internal fun scheduleUpdate(msToNextAlarm: Long) {
    val updateTime = AlarmScheduler.scheduleUpdate(msToNextAlarm, context)
    nextFetch = updateTime.toInstant().toEpochMilli()
    preferences.edit().putLong(NEXT_FETCH, nextFetch).apply()
  }

  override fun addStock(ticker: String): Collection<String> {
    if (tickerList.contains(ticker)) {
      return tickerList
    }
    tickerList.add(ticker)
    save()
    fetch().subscribe(SimpleSubscriber<List<Stock>>())
    return tickerList
  }

  override fun addPosition(ticker: String?, shares: Int, price: Float) {
    if (ticker != null) {
      synchronized(stockList, {
        var position = getStock(ticker)
        if (position == null) {
          position = Stock()
        }
        if (!ticker.contains(ticker)) {
          tickerList.add(ticker)
        }
        position.symbol = ticker
        position.PositionPrice = price
        position.PositionShares = shares
        positionList.remove(position)
        if (shares != 0) {
          position.IsPosition = true
          positionList.add(position)
          stockList.remove(position)
          stockList.add(position)
          save()
          fetch().subscribe(SimpleSubscriber<List<Stock>>())
        } else {
          removePosition(ticker)
        }
      })
    }
  }

  override fun removePosition(ticker: String?) {
    val position = getStock(ticker) ?: return
    position.IsPosition = false
    positionList.remove(position)
    save()
  }

  override fun addStocks(tickers: Collection<String>): Collection<String> {
    tickers
        .filterNot { tickerList.contains(it) }
        .forEach { tickerList.add(it) }
    save()
    fetch().subscribe(SimpleSubscriber<List<Stock>>())
    return tickerList
  }

  override fun removeStock(ticker: String): Collection<String> {
    synchronized(stockList, {
      val ticker2 = "^" + ticker // in case it was an index
      tickerList.remove(ticker)
      tickerList.remove(ticker2)
      val dummy = Stock()
      val dummy2 = Stock()
      dummy.symbol = ticker
      dummy2.symbol = ticker2
      stockList.remove(dummy)
      stockList.remove(dummy2)
      positionList.remove(dummy)
      positionList.remove(dummy2)
      save()
      scheduleUpdate(msToNextAlarm)
      AlarmScheduler.sendBroadcast(context)
      return tickerList
    })
  }

  override fun getStocks(): Collection<Stock> {
    synchronized(stockList, {
      sortStockList()

      val newStockList = ArrayList<Stock>()
      var added: Boolean
      // Set all positions
      for (stock in stockList) {
        added = false
        for (pos in positionList) {
          if (!added && stock.symbol == pos.symbol) {
            stock.IsPosition = true
            stock.PositionShares = pos.PositionShares
            stock.PositionPrice = pos.PositionPrice
            newStockList.add(stock)
            added = true
          }
        }
        if (!added) {
          newStockList.add(stock)
        }
      }
      return newStockList
    })
  }

  internal fun sortStockList() {
    synchronized(stockList, {
      if (Tools.autoSortEnabled()) {
        Collections.sort(stockList)
      } else {
        Collections.sort(stockList) { lhs, rhs ->
          tickerList.indexOf(lhs.symbol).compareTo(tickerList.indexOf(rhs.symbol))
        }
      }
    })
  }

  override fun rearrange(tickers: List<String>): Collection<Stock> {
    tickerList.clear()
    tickerList.addAll(tickers)
    save()
    sendBroadcast()
    return getStocks()
  }

  override fun getStock(ticker: String?): Stock? {
    synchronized(stockList, {
      val dummy = Stock()
      dummy.symbol = ticker
      val index = stockList.indexOf(dummy)
      if (index >= 0) {
        val stock = stockList[index]
        return stock
      }
      return null
    })
  }

  override fun getTickers(): List<String> {
    return ArrayList(tickerList)
  }

  override fun lastFetched(): String {
    val fetched: String
    if (lastFetched > 0L) {
      val instant = Instant.ofEpochMilli(lastFetched)
      val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
      fetched = createTimeString(time)
    } else {
      fetched = ""
    }
    return fetched
  }

  internal fun createTimeString(time: ZonedDateTime): String {
    val fetched: String
    val dfs = DateFormatSymbols.getInstance(Locale.ENGLISH)
    val fetchedDayOfWeek = time.dayOfWeek.value
    val today = ZonedDateTime.now().dayOfWeek.value
    if (today == fetchedDayOfWeek) {
      fetched = Tools.TIME_FORMATTER.format(time)
    } else {
      val day: String = dfs.shortWeekdays[fetchedDayOfWeek % 7 + 1]
      val timeStr: String = Tools.TIME_FORMATTER.format(time)
      fetched = "$timeStr $day"
    }
    return fetched
  }

  override fun nextFetch(): String {
    val fetch: String
    if (nextFetch > 0) {
      val instant = Instant.ofEpochMilli(nextFetch)
      val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
      fetch = createTimeString(time)
    } else {
      fetch = ""
    }
    return fetch
  }
}