package com.github.premnirmal.ticker.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.widget.RemoteViews;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.StocksApp;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.ui.ParanormalActivity;

import javax.inject.Inject;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StockWidget extends AppWidgetProvider {

    public static final String ACTION_NAME = "OPEN_APP";

    @Inject
    IStocksProvider stocksProvider;

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (intent.getAction().equals(ACTION_NAME)) {
            context.startActivity(new Intent(context,ParanormalActivity.class));
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        ((StocksApp)context.getApplicationContext()).inject(this);
        for (final Integer widgetId : appWidgetIds) {
            final Bundle options = appWidgetManager.getAppWidgetOptions(widgetId);
            final int min_width = getMinWidgetWidth(options);
            final RemoteViews remoteViews;
            if (min_width > 250) {
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_4x1);
            } else {
                remoteViews = new RemoteViews(context.getPackageName(),
                        R.layout.widget_2x1);
            }
            updateWidget(context, appWidgetManager, widgetId, remoteViews);
            appWidgetManager.updateAppWidget(widgetId, remoteViews);
        }

        super.onUpdate(context, appWidgetManager, appWidgetIds);
    }

    private int getMinWidgetWidth(Bundle options) {
        if(options == null || !options.containsKey(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)) {
            return 0; // 2x1
        } else {
            return (int) options.get(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH);
        }
    }

    @Override
    public void onAppWidgetOptionsChanged(Context context, AppWidgetManager appWidgetManager, int appWidgetId, Bundle newOptions) {
        ((StocksApp)context.getApplicationContext()).inject(this);
        final int min_width = getMinWidgetWidth(newOptions);
        final RemoteViews remoteViews;
        if (min_width > 250) {
            remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_4x1);
        } else {
            remoteViews = new RemoteViews(context.getPackageName(),
                    R.layout.widget_2x1);
        }
        updateWidget(context, appWidgetManager, appWidgetId, remoteViews);

        super.onAppWidgetOptionsChanged(context, appWidgetManager, appWidgetId, newOptions);
    }

    private void updateWidget(Context context, AppWidgetManager appWidgetManager, int appWidgetId, RemoteViews remoteViews) {
        remoteViews.setRemoteAdapter(R.id.list, new Intent(context, RemoteStockProviderService.class));
        final Intent startActivityIntent = new Intent(context, ParanormalActivity.class);
        final PendingIntent startActivityPendingIntent = PendingIntent.getActivity(context, 0, startActivityIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        remoteViews.setPendingIntentTemplate(R.id.list, startActivityPendingIntent);
        remoteViews.setOnClickPendingIntent(R.id.widget_layout, startActivityPendingIntent);
        remoteViews.setTextViewText(R.id.last_updated, "Last updated: " + stocksProvider.lastFetched());
        appWidgetManager.updateAppWidget(appWidgetId, remoteViews);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.list);
    }
}
