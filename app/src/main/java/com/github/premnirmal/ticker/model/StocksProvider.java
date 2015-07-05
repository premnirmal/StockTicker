package com.github.premnirmal.ticker.model;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.github.premnirmal.ticker.CrashLogger;
import com.github.premnirmal.ticker.RxBus;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.network.StocksApi;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.text.DateFormatSymbols;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Singleton;

import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/21/14.
 */
@Singleton
public class StocksProvider implements IStocksProvider {

    private static final String STOCK_LIST = "STOCK_LIST";
    public static final String SORTED_STOCK_LIST = "SORTED_STOCK_LIST";
    private static final String LAST_FETCHED = "LAST_FETCHED";
    private static final String POSITION_LIST = "POSITION_LIST";

    private static final String DEFAULT_STOCKS = "^SPY,GOOG,AAPL,MSFT,YHOO,TSLA";

    public static final List<String> GOOGLE_SYMBOLS = Arrays.asList(".DJI", ".IXIC");
    public static final List<String> _GOOGLE_SYMBOLS = Arrays.asList("^DJI", "^IXIC");

    private static final Set<String> DEFAULT_SET = new HashSet<String>() {
        {
            add("^SPY");
            add("GOOG");
            add("AAPL");
            add("MSFT");
            add("YHOO");
            add("TSLA");
        }
    };

    private List<String> tickerList;
    private List<Stock> stockList;
    private List<Stock> positionList;
    private String lastFetched;
    private final SharedPreferences preferences;

    private final StocksApi api;
    private final Context context;
    private final RxBus bus;
    private final StocksStorage storage;

    public StocksProvider(StocksApi api, RxBus bus, Context context, SharedPreferences sharedPreferences) {
        this.bus = bus;
        this.api = api;
        this.context = context;
        storage = new StocksStorage(context);
        this.preferences = sharedPreferences;
        final String tickerListVars = preferences.getString(SORTED_STOCK_LIST, DEFAULT_STOCKS);
        tickerList = new ArrayList<>(Arrays.asList(tickerListVars.split(",")));
        final List<String> newTickerList = new ArrayList<>();
        for (String ticker : tickerList) {
            if (ticker != null) {
                newTickerList.add(ticker.replaceAll(".", ""));
            }
        }
        tickerList.removeAll(_GOOGLE_SYMBOLS); // removed google finance because it's causing lots of problems, returning 400s
        if (preferences.contains(STOCK_LIST)) { // for users using older versions
            final Set<String> deprecatedTickerSet = preferences.getStringSet(STOCK_LIST, DEFAULT_SET);
            preferences.edit().remove(STOCK_LIST).apply();
            for (String ticker : deprecatedTickerSet) {
                if (!tickerList.contains(ticker)) {
                    tickerList.add(ticker);
                }
            }
        }

        positionList = Tools.stringToPositions(preferences.getString(POSITION_LIST, ""));

        final String tickerList = Tools.toCommaSeparatedString(this.tickerList);
        preferences.edit().putString(SORTED_STOCK_LIST, tickerList).apply();
        lastFetched = preferences.getString(LAST_FETCHED, null);
        if (lastFetched == null) {
            fetch();
        } else {
            fetchLocal();
        }
    }

    private void fetchLocal() {
        stockList = storage.readSynchronous();
        if (stockList != null) {
            sortStockList();
            sendBroadcast();
            removeGoogleStocks();
        } else {
            fetch();
        }
    }

    private void removeGoogleStocks() {
        if (stockList != null) {
            final Stock dummy1 = new Stock();
            dummy1.symbol = "^DJI";
            final Stock dummy2 = new Stock();
            dummy2.symbol = "^IXIC";
            final Stock dummy3 = new Stock();
            dummy3.symbol = ".DJI";
            final Stock dummy4 = new Stock();
            dummy4.symbol = ".IXIC";
            stockList.remove(dummy1);
            stockList.remove(dummy2);
            stockList.remove(dummy3);
            stockList.remove(dummy4);
        }
    }

    private void save() {
        preferences.edit()
                .remove(STOCK_LIST)
                .putString(POSITION_LIST, Tools.positionsToString(positionList))
                .putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList))
                .putString(LAST_FETCHED, lastFetched)
                .apply();
        removeGoogleStocks();
        storage.save(stockList)
                .subscribe(new Subscriber<Boolean>() {
                    @Override
                    public void onCompleted() {

                    }

                    @Override
                    public void onError(Throwable e) {
                        e.printStackTrace();
                    }

                    @Override
                    public void onNext(Boolean success) {
                        if (!success) {
                            Log.e(getClass().getSimpleName(), "Save failed");
                        }
                    }
                });
    }

    @Override
    public void fetch() {
        if (Tools.isNetworkOnline(context)) {
            api.getStocks(tickerList)
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<List<Stock>>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            CrashLogger.logException(new RuntimeException("Encountered onError when fetching stocks", e)); // why does this happen?
                            e.printStackTrace();
                            scheduleUpdate(SystemClock.elapsedRealtime() + (60 * 1000)); // 1 minute
                        }

                        @Override
                        public void onNext(List<Stock> stocks) {
                            if (stocks == null || stocks.isEmpty()) {
                                onError(new NullPointerException("stocks == null"));
                            } else {
                                stockList = stocks;
                                lastFetched = api.lastFetched;
                                save();
                                sendBroadcast();
                            }
                        }
                    });
        } else {
            scheduleUpdate(SystemClock.elapsedRealtime() + (5 * 60 * 1000)); // 5 minutes
        }
    }

    private void sendBroadcast() {
        AlarmScheduler.sendBroadcast(context);
        bus.post(new StockUpdatedEvent());
        scheduleUpdate(getMsToNextAlarm());
    }

    /**
     * Takes care of weekends and afterhours
     *
     * @return
     */
    private long getMsToNextAlarm() {
        return AlarmScheduler.msToNextAlarm(context, preferences);
    }

    private void scheduleUpdate(long msToNextAlarm) {
        AlarmScheduler.scheduleUpdate(msToNextAlarm, context, preferences);
    }

    @Override
    public Collection<String> addStock(String ticker) {
        if (tickerList.contains(ticker)) {
            return tickerList;
        }
        tickerList.add(ticker);
        save();
        fetch();
        return tickerList;
    }

    @Override
    public Collection<String> addPosition(String ticker, float shares, float price) {
        Stock position = getStock(ticker);
        if (position == null) {
            position = new Stock();
        } else {

        }
        if(!ticker.contains(ticker)) {
            tickerList.add(ticker);
        }
        position.symbol = ticker;
        position.IsPosition = true;
        position.PositionPrice = price;
        position.PositionShares = shares;
        positionList.remove(position);
        positionList.add(position);
        stockList.remove(position);
        stockList.add(position);
        save();
        fetch();
        return tickerList;
    }

    @Override
    public Collection<String> addStocks(Collection<String> tickers) {
        for (String ticker : tickers) {
            if (!tickerList.contains(ticker)) {
                tickerList.add(ticker);
            }
        }
        save();
        fetch();
        return tickerList;
    }

    @Override
    public Collection<String> removeStock(String ticker) {
        tickerList.remove(ticker);
        final Stock dummy = new Stock();
        dummy.symbol = ticker;
        stockList.remove(dummy);
        positionList.remove(dummy);
        save();
        sendBroadcast();
        return tickerList;
    }

    @Override
    public Collection<Stock> getStocks() {
        if (stockList != null) {
            sortStockList();

            List<Stock> newStockList = new ArrayList<Stock>();
            boolean added = false;
            // Set all positions
            for (Stock stock : stockList) {
                added = false;
                for (Stock pos : positionList) {
                    if (added == false && stock.symbol.equals(pos.symbol)) {
                        stock.IsPosition = true;
                        stock.PositionShares = pos.PositionShares;
                        stock.PositionPrice = pos.PositionPrice;
                        newStockList.add(stock);
                        added = true;
                    }
                }
                if (added == false) {
                    newStockList.add(stock);
                }
            }

            return newStockList;
        }
        return stockList;
    }

    private void sortStockList() {
        if (Tools.autoSortEnabled()) {
            Collections.sort(stockList);
        } else {
            Collections.sort(stockList, new Comparator<Stock>() {
                @Override
                public int compare(Stock lhs, Stock rhs) {
                    return ((Integer) tickerList.indexOf(lhs.symbol))
                            .compareTo(tickerList.indexOf(rhs.symbol));
                }
            });
        }
    }

    @Override
    public Collection<Stock> rearrange(List<String> tickers) {
        tickerList = new ArrayList<>(tickers);
        save();
        sendBroadcast();
        return getStocks();
    }

    @Override
    public Stock getStock(String ticker) {
        final Stock dummy = new Stock();
        dummy.symbol = ticker;
        final int index = stockList.indexOf(dummy);
        if (index >= 0) {
            final Stock stock = stockList.get(index);
            return stock;
        }
        return null;
    }

    @Override
    public List<String> getTickers() {
        return new ArrayList<>(tickerList);
    }

    @Override
    public String lastFetched() {
        final String fetched;
        if (!TextUtils.isEmpty(lastFetched)) {
            final DateTime time = DateTime.parse(lastFetched)
                    .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()));
            final DateFormatSymbols dfs = DateFormatSymbols.getInstance(Locale.ENGLISH);
            final int fetchedDayOfWeek = time.dayOfWeek().get();
            final int today = DateTime.now().dayOfWeek().get();
            if (today == fetchedDayOfWeek) {
                fetched = time.toString(ISODateTimeFormat.hourMinute());
            } else {
                fetched = dfs.getWeekdays()[fetchedDayOfWeek % 7 + 1] + " "
                        + (time.toString(ISODateTimeFormat.hourMinute()));
            }
        } else {
            fetched = "";
        }
        return fetched;
    }

}
