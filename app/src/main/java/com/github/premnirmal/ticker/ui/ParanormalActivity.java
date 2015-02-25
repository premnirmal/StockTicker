package com.github.premnirmal.ticker.ui;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.ticker.BaseActivity;
import com.github.premnirmal.ticker.Injector;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.NoNetworkEvent;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.settings.SettingsActivity;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.R;
import com.terlici.dragndroplist.DragNDropListView;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ParanormalActivity extends BaseActivity {

    @Inject
    IStocksProvider stocksProvider;

    @Inject
    EventBus bus;

    private final Handler handler = new Handler();
    private AlertDialog alertDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(Tools.SETTING_AUTOSORT, false)) {
            setContentView(R.layout.activity_paranormal);
        } else {
            setContentView(R.layout.activity_paranormal_draggable);
        }
        try {
            bus.register(this);
        } catch (NoClassDefFoundError e) {
            // https://github.com/greenrobot/EventBus/issues/149
            // pre lollipop https://github.com/square/otto/issues/139
            Crashlytics.logException(e);
        }
        if (!Tools.isNetworkOnline(getApplicationContext())) {
            onEvent(new NoNetworkEvent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        final MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_paranormal, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_add_ticker) {
            final Intent intent = new Intent(this, TickerSelectorActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_settings) {
            final Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (item.getItemId() == R.id.action_update) {
            if (!Tools.isNetworkOnline(getApplicationContext())) {
                onEvent(new NoNetworkEvent());
            } else {
                stocksProvider.fetch();
                item.setActionView(new ProgressBar(this));
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
    }

    private void update() {
        supportInvalidateOptionsMenu();
        if (stocksProvider.getStocks() == null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            }, 600);
        }
        final SharedPreferences preferences = getSharedPreferences(Tools.PREFS_NAME, MODE_PRIVATE);
        if (preferences.getBoolean(Tools.SETTING_AUTOSORT, false)) {
            setContentView(R.layout.activity_paranormal);
        } else {
            setContentView(R.layout.activity_paranormal_draggable);
        }

        ((TextView) findViewById(R.id.last_updated)).setText("Last updated: " + stocksProvider.lastFetched());

        final AdapterView adapterView = (AdapterView) findViewById(R.id.stockList);
        final StocksAdapter adapter = new StocksAdapter(stocksProvider,
                preferences.getBoolean(Tools.SETTING_AUTOSORT, false));
        if (!preferences.getBoolean(Tools.SETTING_AUTOSORT, false)
                && adapterView instanceof DragNDropListView) {
            ((DragNDropListView) adapterView).setDragNDropAdapter(adapter);
        } else {
            adapterView.setAdapter(adapter);
        }
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
                final Intent intent = new Intent(ParanormalActivity.this, GraphActivity.class);
                intent.putExtra(GraphActivity.GRAPH_DATA, adapter.getItem(position));
                startActivity(intent);
            }
        });

        adapterView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, final int position, long id) {
                new AlertDialog.Builder(ParanormalActivity.this)
                        .setMessage("Remove stock?")
                        .setPositiveButton("Remove", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                stocksProvider.removeStock(adapter.getItem(position).symbol);
                                update();
                            }
                        })
                        .show();
                update();
                return true;
            }
        });
    }

    @Override
    protected void onDestroy() {
        bus.unregister(this);
        super.onDestroy();
    }

    public void onEvent(StockUpdatedEvent event) {
        update();
    }

    public void onEvent(NoNetworkEvent event) {
        final boolean showing = alertDialog != null && !alertDialog.isShowing();
        if (!showing) {
            alertDialog = showDialog(getString(R.string.no_network_message));
        }
    }

    @Override
    public void onBackPressed() {

        final Bundle extras = getIntent().getExtras();
        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        final Intent intent = new Intent(getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(this);
        final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(this, StockWidget.class));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list);
        }
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        sendBroadcast(intent);

        final Intent result = new Intent();
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
        setResult(RESULT_OK, result);

        super.onBackPressed();
    }
}
