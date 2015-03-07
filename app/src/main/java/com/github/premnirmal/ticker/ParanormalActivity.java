package com.github.premnirmal.ticker;

import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.FragmentManager;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.R;

import io.fabric.sdk.android.Fabric;

public class ParanormalActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_paranormal);
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                setRandomActionBarColor();
            }
        });
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
