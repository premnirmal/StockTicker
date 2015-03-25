package com.github.premnirmal.ticker.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;

import com.github.premnirmal.ticker.RefreshReceiver;
import com.github.premnirmal.ticker.RxBus;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.network.Query;
import com.github.premnirmal.ticker.network.QueryCreator;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.network.StockQuery;
import com.github.premnirmal.ticker.network.StocksApi;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;
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
import rx.functions.Func1;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/21/14.
 */
@Singleton
public class StocksProvider implements IStocksProvider {

    public static final String STOCK_LIST = "STOCK_LIST";
    public static final String SORTED_STOCK_LIST = "SORTED_STOCK_LIST";
    public static final String LAST_FETCHED = "LAST_FETCHED";
    public static final String UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE";

    private static final String DEFAULT_STOCKS = "^SPY,GOOG,AAPL,MSFT";

    private static final Set<String> DEFAULT_SET = new HashSet<String>() {
        {
            add("^SPY");
            add("GOOG");
            add("AAPL");
            add("MSFT");
            add("YHOO");
        }
    };

    private Set<String> deprecatedTickerSet;
    private List<String> tickerList;
    private List<Stock> stockList;
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
        if (preferences.contains(STOCK_LIST)) { // for users using older versions
            deprecatedTickerSet = preferences.getStringSet(STOCK_LIST, DEFAULT_SET);
            preferences.edit().remove(STOCK_LIST).apply();
            for (String ticker : deprecatedTickerSet) {
                if (!tickerList.contains(ticker)) {
                    tickerList.add(ticker);
                }
            }
            preferences.edit().putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList)).apply();
        }
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
        } else {
            fetch();
        }
    }

    private void save() {
        preferences.edit()
                .remove(STOCK_LIST)
                .putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList))
                .putString(LAST_FETCHED, lastFetched)
                .apply();
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
            api.getStocks(QueryCreator.buildStocksQuery(tickerList.toArray()))
                    .map(new Func1<StockQuery, Query>() {
                        @Override
                        public Query call(StockQuery stockQuery) {
                            if (stockQuery == null) {
                                return null;
                            }
                            final Query query = stockQuery.query;
                            stockList = query.results.quote;
                            lastFetched = query.created;
                            save();
                            return query;
                        }
                    })
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .subscribe(new Subscriber<Query>() {
                        @Override
                        public void onCompleted() {
                        }

                        @Override
                        public void onError(Throwable e) {
                            fetch();
                        }

                        @Override
                        public void onNext(Query response) {
                            sendBroadcast();
                        }
                    });
        } else {
            scheduleUpdate(SystemClock.elapsedRealtime() + (AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3)); // 5 minutes
        }
    }

    private void sendBroadcast() {
        final Intent intent = new Intent(context.getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, StockWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
        bus.post(new StockUpdatedEvent());
        scheduleUpdate(getMsToNextAlarm());
    }

    /**
     * Takes care of weekends and afterhours
     *
     * @return
     */
    private long getMsToNextAlarm() {
        final int hourOfDay = DateTime.now().hourOfDay().get();
        final int minuteOfHour = DateTime.now().minuteOfHour().get();
        final int dayOfWeek = DateTime.now().getDayOfWeek();

        final int[] startTimez = Tools.startTime();
        final int[] endTimez = Tools.endTime();

        final MutableDateTime mutableDateTime = new MutableDateTime(DateTime.now());
        mutableDateTime.setZone(DateTimeZone.getDefault());

        boolean set = false;

        if (hourOfDay > endTimez[0] || (hourOfDay == endTimez[0] && minuteOfHour > endTimez[1])) {
            mutableDateTime.addDays(1);
            mutableDateTime.setHourOfDay(startTimez[0]);
            mutableDateTime.setMinuteOfHour(startTimez[1]);
            set = true;
        }

        if (set && dayOfWeek == DateTimeConstants.FRIDAY) {
            mutableDateTime.addDays(2);
        }

        if (dayOfWeek > DateTimeConstants.FRIDAY) {
            if (dayOfWeek == DateTimeConstants.SATURDAY) {
                mutableDateTime.addDays(set ? 1 : 2);
            } else if (dayOfWeek == DateTimeConstants.SUNDAY) {
                if (!set) {
                    mutableDateTime.addDays(1);
                }
            }
            if (!set) {
                set = true;
                mutableDateTime.setHourOfDay(startTimez[0]);
                mutableDateTime.setMinuteOfHour(startTimez[1]);
            }
        }
        if (set) {
            final long msToNextSchedule = mutableDateTime.getMillis() - DateTime.now().getMillis();
            return SystemClock.elapsedRealtime() + msToNextSchedule;
        } else {
            final int updatePref = preferences.getInt(Tools.UPDATE_INTERVAL, 1);
            final long time = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (updatePref + 1);
            return SystemClock.elapsedRealtime() + time;
        }
    }

    private void scheduleUpdate(long msToNextAlarm) {
        final Intent updateReceiverIntent = new Intent(context, RefreshReceiver.class);
        updateReceiverIntent.setAction(UPDATE_FILTER);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, updateReceiverIntent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, msToNextAlarm, pendingIntent);
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
        final String sanitizedTicker = ticker.replace("^", "");
        tickerList.remove(sanitizedTicker);
        tickerList.remove("^" + sanitizedTicker);
        final Stock dummy = new Stock();
        dummy.symbol = sanitizedTicker;
        stockList.remove(dummy);
        save();
        sendBroadcast();
        return tickerList;
    }

    @Override
    public Collection<Stock> getStocks() {
        if (stockList != null) {
            sortStockList();
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
                fetched = "Today " + (time.toString(ISODateTimeFormat.hourMinute()));
            } else {
                fetched = dfs.getWeekdays()[fetchedDayOfWeek % 7 + 1] + " "
                        + (time.toString(ISODateTimeFormat.hourMinute()));
            }
        } else {
            fetched = "Unavailable";
        }
        return fetched;
    }

}
