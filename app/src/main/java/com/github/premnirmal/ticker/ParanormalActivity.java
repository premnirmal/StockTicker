package com.github.premnirmal.ticker;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.BuildConfig;
import com.github.premnirmal.tickerwidget.R;
import com.tapjoy.Tapjoy;

import javax.inject.Inject;

import io.fabric.sdk.android.Fabric;

public class ParanormalActivity extends BaseActivity {

    @Inject
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Tapjoy.connect(this, BuildConfig.KEY);
        Tapjoy.setDebugEnabled(BuildConfig.DEBUG);
        Injector.inject(this);
        Fabric.with(this, new Crashlytics());
        setContentView(R.layout.activity_paranormal);
        if (preferences.getBoolean(Tools.WHATS_NEW, false)) {
            preferences.edit().putBoolean(Tools.WHATS_NEW, false).apply();
            final StringBuilder stringBuilder = new StringBuilder();
            final String[] whatsNew = getResources().getStringArray(R.array.whats_new);
            for (int i = 0; i < whatsNew.length; i++) {
                stringBuilder.append("\t");
                stringBuilder.append(whatsNew[i]);
                if (i != whatsNew.length - 1) {
                    stringBuilder.append("\n\n");
                }
            }
            new AlertDialog.Builder(this)
                    .setTitle("What\'s new in Version " + BuildConfig.VERSION_NAME)
                    .setMessage(stringBuilder.toString())
                    .setNeutralButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                        }
                    })
                    .show();
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
