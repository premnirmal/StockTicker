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
import android.widget.Toast;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.NoNetworkEvent;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.network.Stock;
import com.github.premnirmal.ticker.network.StockQuery;
import com.github.premnirmal.ticker.network.StocksApi;
import com.github.premnirmal.ticker.widget.StockWidget;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.format.ISODateTimeFormat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimeZone;

import javax.inject.Singleton;

import de.greenrobot.event.EventBus;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

/**
 * Created by premnirmal on 12/21/14.
 */
@Singleton
public class StocksProvider implements IStocksProvider {

    public static final String STOCK_LIST = "STOCK_LIST";
    public static final String LAST_FETCHED = "LAST_FETCHED";

    private static final Set<String> DEFAULT_LIST = new HashSet<String>() {
        {
            add("^SPY");
            add("GOOG");
            add("AAPL");
            add("MSFT");
        }
    };

    private Set<String> tickerList;
    private List<Stock> stockList;
    private String lastFetched;
    private final SharedPreferences preferences;

    private final StocksApi api;
    private final Context context;
    private final EventBus bus;

    public StocksProvider(StocksApi api, EventBus bus, Context context) {
        this.bus = bus;
        this.api = api;
        this.context = context;
        preferences = context.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);
        tickerList = preferences.getStringSet(STOCK_LIST, DEFAULT_LIST);
        lastFetched = preferences.getString(LAST_FETCHED, null);
        tickerList.addAll(DEFAULT_LIST);
        fetch();
    }

    private void save() {
        preferences.edit().remove(STOCK_LIST).commit();
        preferences.edit().putStringSet(STOCK_LIST, tickerList).commit();
        preferences.edit().putString(LAST_FETCHED, lastFetched).commit();
    }

    @Override
    public void fetch() {
        if(Tools.isNetworkOnline(context)) {
            api.getStocks(Tools.buildQuery(tickerList.toArray()))
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeOn(Schedulers.io())
                    .doOnError(new Action1<Throwable>() {
                        @Override
                        public void call(Throwable throwable) {
                            Toast.makeText(context, "Error", Toast.LENGTH_SHORT).show();
                        }
                    })
                    .subscribe(new Action1<StockQuery>() {
                        @Override
                        public void call(StockQuery response) {
                            try {
                                stockList = response.query.results.quote;
                                lastFetched = response.query.created;
                                sendBroadcast();
                            } catch (NullPointerException e) {
                                fetch();
                            }
                        }
                    });
        } else {
            bus.post(new NoNetworkEvent());
        }
    }

    private void sendBroadcast() {
        final Intent intent = new Intent(context.getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, StockWidget.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
        bus.post(new StockUpdatedEvent());

        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, intent, 0);
        alarmManager.cancel(pendingIntent);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_FIFTEEN_MINUTES, pendingIntent);

    }

    @Override
    public Collection<String> addStock(String ticker) {
        tickerList.add(ticker);
        save();
        fetch();
        return tickerList;
    }

    @Override
    public Collection<String> addStocks(Collection<String> tickers) {
        tickerList.addAll(tickers);
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
            Collections.sort(stockList);
        }
        return stockList;
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
