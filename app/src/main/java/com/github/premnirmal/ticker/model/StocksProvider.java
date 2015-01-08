package com.github.premnirmal.ticker.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.github.premnirmal.ticker.network.QueryCreator;
import com.github.premnirmal.tickerwidget.R;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.UpdateReceiver;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.network.Query;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.network.StockQuery;
import com.github.premnirmal.ticker.network.StocksApi;
import com.github.premnirmal.ticker.widget.StockWidget;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import rx.Subscriber;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
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
    private final EventBus bus;
    private final StocksStorage storage;

    public StocksProvider(StocksApi api, EventBus bus, Context context) {
        this.bus = bus;
        this.api = api;
        this.context = context;
        storage = new StocksStorage(context);
        preferences = context.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);
        final String tickerListVars = preferences.getString(SORTED_STOCK_LIST, DEFAULT_STOCKS);
        tickerList = new ArrayList<>(Arrays.asList(tickerListVars.split(",")));
        if (preferences.contains(STOCK_LIST)) { // for users using older versions
            deprecatedTickerSet = preferences.getStringSet(STOCK_LIST, DEFAULT_SET);
            preferences.edit().remove(STOCK_LIST).commit();
            for (String ticker : deprecatedTickerSet) {
                if (!tickerList.contains(ticker)) {
                    tickerList.add(ticker);
                }
            }
            preferences.edit().putString(SORTED_STOCK_LIST, Tools.toCommaSeparatedString(tickerList)).commit();
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
                .commit();
        storage.save(stockList)
                .doOnError(new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        throwable.printStackTrace();
                    }
                })
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
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .map(new Func1<StockQuery, Query>() {
                        @Override
                        public Query call(StockQuery stockQuery) {
                            if (stockQuery == null) {
                                return null;
                            }
                            return stockQuery.query;
                        }
                    })
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
                            stockList = response.results.quote;
                            lastFetched = response.created;
                            save();
                            sendBroadcast();
                        }
                    });
        } else {
            scheduleUpdate(SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES);
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
        scheduleUpdate(Tools.getMsToNextAlarm());
    }

    private void scheduleUpdate(long msToNextAlarm) {
        final Intent updateReceiverIntent = new Intent(context, UpdateReceiver.class);
        updateReceiverIntent.setAction(UPDATE_FILTER);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, updateReceiverIntent, 0);
        if (Build.VERSION.SDK_INT >= 19) {
            alarmManager.setWindow(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    msToNextAlarm,
                    AlarmManager.INTERVAL_FIFTEEN_MINUTES / 3, // 5 minute window
                    pendingIntent);
        } else {
            alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                    msToNextAlarm,
                    pendingIntent);
        }
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
        if (preferences.getBoolean(Tools.SETTING_AUTOSORT, false)) {
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
        if (!TextUtils.isEmpty(lastFetched)) {
            return DateTime.parse(lastFetched)
                    .withZone(DateTimeZone.forTimeZone(TimeZone.getDefault()))
                    .toString(ISODateTimeFormat.hourMinute());
        }
        return lastFetched;
    }

}
