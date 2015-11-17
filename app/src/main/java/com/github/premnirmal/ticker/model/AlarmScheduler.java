package com.github.premnirmal.ticker.model;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.SystemClock;

import com.github.premnirmal.ticker.Analytics;
import com.github.premnirmal.ticker.RefreshReceiver;
import com.github.premnirmal.ticker.Tools;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

/**
 * Created by premnirmal on 4/10/15.
 */
public final class AlarmScheduler {

    public static final String UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE";

    public static long msOfNextAlarm() {
        return SystemClock.elapsedRealtime() + msToNextAlarm();
    }

    public static long msToNextAlarm() {
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
        final long msToNextAlarm;
        if(set) {
            msToNextAlarm = mutableDateTime.getMillis() - DateTime.now().getMillis();
        } else {
            msToNextAlarm = Tools.getUpdateInterval();
        }
        return msToNextAlarm;
    }

    static void scheduleUpdate(long msToNextAlarm, Context context) {
        Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION, "UpdateScheduled for " + ((msToNextAlarm - SystemClock.elapsedRealtime())/(1000*60)) + " minutes");
        final Intent updateReceiverIntent = new Intent(context, RefreshReceiver.class);
        updateReceiverIntent.setAction(UPDATE_FILTER);
        final AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        final PendingIntent pendingIntent = PendingIntent.getBroadcast(context.getApplicationContext(), 0, updateReceiverIntent, 0);
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, msToNextAlarm, pendingIntent);
    }

    static void sendBroadcast(Context context) {
        final Intent intent = new Intent(context.getApplicationContext(), StockWidget.class);
        intent.setAction(AppWidgetManager.ACTION_APPWIDGET_UPDATE);
        final AppWidgetManager widgetManager = AppWidgetManager.getInstance(context);
        final int[] ids = widgetManager.getAppWidgetIds(new ComponentName(context, StockWidget.class));
        widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list);
        intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids);
        context.sendBroadcast(intent);
    }

}
