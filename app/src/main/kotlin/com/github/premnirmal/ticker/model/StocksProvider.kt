package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.SharedPreferences
import android.os.SystemClock
import android.text.TextUtils
import android.util.Log
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.CrashLogger
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.events.NoNetworkEvent
import com.github.premnirmal.ticker.events.UpdateFailedEvent
import com.github.premnirmal.ticker.events.StockUpdatedEvent
import com.github.premnirmal.ticker.network.Stock
import com.github.premnirmal.ticker.network.StocksApi
import org.joda.time.DateTime
import org.joda.time.DateTimeZone
import org.joda.time.format.ISODateTimeFormat
import rx.Observable
import rx.Subscriber
import rx.android.schedulers.AndroidSchedulers
import rx.functions.Func1
import rx.schedulers.Schedulers
import java.text.DateFormatSymbols
import java.util.*
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class StocksProvider(private val api: StocksApi, private val bus: RxBus, private val context: Context, private val preferences: SharedPreferences) : IStocksProvider {

    private val tickerList: MutableList<String>
    private val stockList: MutableList<Stock> = ArrayList<Stock>()
    private val positionList: MutableList<Stock>
    private var lastFetched: String? = null
    private val storage: StocksStorage

    init {
        storage = StocksStorage(context)
        val tickerListVars = preferences.getString(SORTED_STOCK_LIST, DEFAULT_STOCKS)
        tickerList = ArrayList(Arrays.asList(*tickerListVars.split(",".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()))
        val newTickerList = ArrayList<String>()
        for (ticker in tickerList) {
            newTickerList.add(ticker.replace(".".toRegex(), ""))
        }
        tickerList.removeAll(_GOOGLE_SYMBOLS) // removed google finance because it's causing lots of problems, returning 400s
        if (preferences.contains(STOCK_LIST)) {
            // for users using older versions
            val deprecatedTickerSet = preferences.getStringSet(STOCK_LIST, DEFAULT_SET)
            preferences.edit().remove(STOCK_LIST).apply()
            for (ticker in deprecatedTickerSet) {
                if (!tickerList.contains(ticker)) {
                    tickerList.add(ticker)
                }
            }
        }

        positionList = Tools.stringToPositions(preferences.getString(POSITION_LIST, ""))

        val tickerList = Tools.toCommaSeparatedString(this.tickerList)
        preferences.edit().putString(SORTED_STOCK_LIST, tickerList).apply()
        lastFetched = preferences.getString(LAST_FETCHED, null)
        if (lastFetched == null) {
            fetch()
        } else {
            fetchLocal()
        }
    }

    private fun fetchLocal() {
        stockList.clear()
        stockList.addAll(storage.readSynchronous())
        if (!stockList.isEmpty()) {
            sortStockList()
            sendBroadcast()
            removeGoogleStocks()
        } else {
            fetch()
        }
    }

    private fun removeGoogleStocks() {
        val dummy1 = Stock()
        dummy1.symbol = "^DJI"
        val dummy2 = Stock()
        dummy2.symbol = "^IXIC"
        val dummy3 = Stock()
        dummy3.symbol = ".DJI"
        val dummy4 = Stock()
        dummy4.symbol = ".IXIC"
        stockList.remove(dummy1)
        stockList.remove(dummy2)
        stockList.remove(dummy3)
        stockList.remove(dummy4)
    }

    private fun save() {
        preferences.edit().remove(STOCK_LIST).putString(POSITION_LIST, Tools.positionsToString(positionList)).putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList)).putString(LAST_FETCHED, lastFetched).apply()
        removeGoogleStocks()
        storage.save(stockList).subscribe(object : Subscriber<Boolean>() {
            override fun onCompleted() {

            }

            override fun onError(e: Throwable) {
                e.printStackTrace()
            }

            override fun onNext(success: Boolean) {
                if (!success) {
                    Log.e(javaClass.simpleName, "Save failed")
                }
            }
        })
    }

    override fun fetch() {
        if (Tools.isNetworkOnline(context)) {
            api.getStocks(tickerList).observeOn(AndroidSchedulers.mainThread()).subscribeOn(Schedulers.io())
                    .subscribe(object : Subscriber<List<Stock>>() {
                        override fun onCompleted() {
                        }

                        override fun onError(e: Throwable) {
                            CrashLogger.logException(RuntimeException("Encountered onError when fetching stocks", e)) // why does this happen?
                            e.printStackTrace()
                            bus.post(UpdateFailedEvent())
                            scheduleUpdate(SystemClock.elapsedRealtime() + 60 * 1000) // 1 minute
                        }

                        override fun onNext(stocks: List<Stock>?) {
                            if (stocks == null || stocks.isEmpty()) {
                                onError(NullPointerException("stocks == null or empty"))
                            } else {
                                stockList.clear()
                                stockList.addAll(stocks)
                                lastFetched = api.lastFetched
                                save()
                                sendBroadcast()
                            }
                        }
                    })
        } else {
            bus.post(NoNetworkEvent())
            scheduleUpdate(SystemClock.elapsedRealtime() + 5 * 60 * 1000) // 5 minutes
        }
    }

    private fun sendBroadcast() {
        AlarmScheduler.sendBroadcast(context)
        bus.post(StockUpdatedEvent())
        scheduleUpdate(msToNextAlarm)
    }

    /**
     * Takes care of weekends and afterhours

     * @return
     */
    private val msToNextAlarm: Long
        get() = AlarmScheduler.msOfNextAlarm()

    private fun scheduleUpdate(msToNextAlarm: Long) {
        AlarmScheduler.scheduleUpdate(msToNextAlarm, context)
    }

    override fun addStock(ticker: String): Collection<String> {
        if (tickerList.contains(ticker)) {
            return tickerList
        }
        tickerList.add(ticker)
        save()
        fetch()
        return tickerList
    }

    override fun addPosition(ticker: String?, shares: Int, price: Float) {
        if (ticker != null) {
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
                fetch()
            } else {
                removePosition(ticker)
            }
        }
    }

    override fun removePosition(ticker: String?) {
        val position = getStock(ticker) ?: return
        position.IsPosition = false
        positionList.remove(position)
        save()
    }

    override fun addStocks(tickers: Collection<String>): Collection<String> {
        for (ticker in tickers) {
            if (!tickerList.contains(ticker)) {
                tickerList.add(ticker)
            }
        }
        save()
        fetch()
        return tickerList
    }

    override fun removeStock(ticker: String): Collection<String> {
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
        AlarmScheduler.sendBroadcast(context)
        scheduleUpdate(msToNextAlarm)
        return tickerList
    }

    override fun getStocks(): Collection<Stock> {
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
    }

    private fun sortStockList() {
        if (Tools.autoSortEnabled()) {
            Collections.sort(stockList)
        } else {
            Collections.sort(stockList) { lhs, rhs -> tickerList.indexOf(lhs.symbol).toInt().compareTo(tickerList.indexOf(rhs.symbol)) }
        }
    }

    override fun rearrange(tickers: List<String>): Collection<Stock> {
        tickerList.clear()
        tickerList.addAll(tickers)
        save()
        sendBroadcast()
        return getStocks()
    }

    override fun getStock(ticker: String?): Stock? {
        val dummy = Stock()
        dummy.symbol = ticker
        val index = stockList.indexOf(dummy)
        if (index >= 0) {
            val stock = stockList[index]
            return stock
        }
        return null
    }

    override fun getTickers(): List<String> {
        return ArrayList(tickerList)
    }

    override fun lastFetched(): String {
        val fetched: String
        if (!TextUtils.isEmpty(lastFetched)) {
            val time = DateTime.parse(lastFetched).withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
            val dfs = DateFormatSymbols.getInstance(Locale.ENGLISH)
            val fetchedDayOfWeek = time.dayOfWeek().get()
            val today = DateTime.now().dayOfWeek().get()
            if (today == fetchedDayOfWeek) {
                fetched = time.toString(ISODateTimeFormat.hourMinute())
            } else {
                val day: String = dfs.weekdays[fetchedDayOfWeek % 7 + 1]
                val timeStr: String = time.toString(ISODateTimeFormat.hourMinute())
                fetched = "$day $timeStr"
            }
        } else {
            fetched = ""
        }
        return fetched
    }

    companion object {

        private val STOCK_LIST = "STOCK_LIST"
        private val LAST_FETCHED = "LAST_FETCHED"
        private val POSITION_LIST = "POSITION_LIST"
        private val DEFAULT_STOCKS = "^SPY,GOOG,AAPL,MSFT,YHOO,TSLA"

        @JvmField val SORTED_STOCK_LIST = "SORTED_STOCK_LIST"
        @JvmField val GOOGLE_SYMBOLS = Arrays.asList(".DJI", ".IXIC")
        @JvmField val _GOOGLE_SYMBOLS = Arrays.asList("^DJI", "^IXIC")

        private val DEFAULT_SET = object : HashSet<String>() {
            init {
                add("^SPY")
                add("GOOG")
                add("AAPL")
                add("MSFT")
                add("YHOO")
                add("TSLA")
            }
        }
    }

}