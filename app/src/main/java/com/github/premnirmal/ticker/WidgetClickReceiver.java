package com.github.premnirmal.ticker;

import android.appwidget.AppWidgetManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

import com.github.premnirmal.ticker.widget.StockWidget;

/**
 * Created by premnirmal on 10/30/15.
 */
public class WidgetClickReceiver extends BroadcastReceiver {

    public static final String CLICK_BCAST_INTENTFILTER = "com.github.premnirmal.ticker.widgetclick";
    public static final String FLIP = "FLIP";

    @Override
    public void onReceive(Context context, Intent intent) {
        if(intent.getBooleanExtra(FLIP, false)) {
            Tools.flipChange();
            final Intent updateIntent = new Intent(context, StockWidget.class);
            updateIntent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
            final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
            final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, StockWidget.class));
            updateIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
            context.sendBroadcast(updateIntent);
        } else {
            final Intent startActivityIntent = new Intent(context, ParanormalActivity.class);
            startActivityIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(startActivityIntent);
        }
    }
}
