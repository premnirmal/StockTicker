package com.github.premnirmal.ticker.ui;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.events.NoNetworkEvent;
import com.github.premnirmal.ticker.events.StockUpdatedEvent;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.settings.SettingsActivity;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.google.common.eventbus.Subscribe;

import javax.inject.Inject;

import de.greenrobot.event.EventBus;

public class ParanormalActivity extends ActionBarActivity {

    @Inject
    IStocksProvider stocksProvider;
    @Inject
    EventBus bus;

    private final Handler handler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ((StocksApp) getApplicationContext()).inject(this);
        setContentView(R.layout.activity_paranormal);
        bus.register(this);
        if(!Tools.isNetworkOnline(getApplicationContext())) {
            onEvent(new NoNetworkEvent());
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
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
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        update();
    }

    private void update() {
        if (stocksProvider.getStocks() == null) {
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    update();
                }
            }, 600);
        }

        final AdapterView adapterView = (AdapterView) findViewById(R.id.stockList);
        final StocksAdapter adapter = new StocksAdapter(stocksProvider);
        adapterView.setAdapter(adapter);
        adapterView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, final int position, long id) {
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
            }
        });
    }

    @Override
    protected void onDestroy() {
        bus.unregister(this);
        super.onDestroy();
    }

    @Subscribe
    public void onEvent(StockUpdatedEvent event) {
        update();
    }


    boolean showing = false;
    @Subscribe
    public void onEvent(NoNetworkEvent event) {
        if(!showing) {
            new AlertDialog.Builder(this)
                    .setMessage(R.string.no_network_message)
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            showing = false;
                        }
                    })
                    .show();
            showing = true;
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
