package com.github.premnirmal.ticker;

import android.app.AlertDialog;
import android.appwidget.AppWidgetManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;

import com.github.premnirmal.tickerwidget.BuildConfig;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Inject;

public class ParanormalActivity extends BaseActivity {

    @Inject
    SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Injector.inject(this);
        final Bundle extras = getIntent().getExtras();
        int widgetId = AppWidgetManager.INVALID_APPWIDGET_ID;
        if (extras != null) {
            widgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }
        if(widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
            final Intent result = new Intent();
            result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId);
            setResult(RESULT_OK, result);
        }
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
    protected void onResume() {
        super.onResume();
        supportInvalidateOptionsMenu();
    }
}
