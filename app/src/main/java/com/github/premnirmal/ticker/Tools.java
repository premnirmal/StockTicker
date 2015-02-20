package com.github.premnirmal.ticker;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;
import android.os.SystemClock;

import com.github.premnirmal.tickerwidget.R;

import org.joda.time.DateTime;
import org.joda.time.DateTimeConstants;
import org.joda.time.DateTimeZone;
import org.joda.time.MutableDateTime;

import java.io.File;
import java.util.List;

/**
 * Created by premnirmal on 12/22/14.
 */
public final class Tools {

    public static final String PREFS_NAME = "com.github.premnirmal.ticker";
    public static final String FONT_SIZE = "com.github.premnirmal.ticker.textsize";
    public static final String SETTING_AUTOSORT = "SETTING_AUTOSORT";
    public static final String WIDGET_BG = "WIDGET_BG";
    public static final String UPDATE_INTERVAL = "UPDATE_INTERVAL";
    public static final int TRANSPARENT = 0;
    public static final int TRANSLUCENT = 1;

    public static int getBackgroundColor(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(WIDGET_BG, TRANSPARENT) == TRANSPARENT ? TRANSPARENT
                : context.getResources().getColor(R.color.translucent);
    }

    public static float getFontSize(Context context) {
        final int size = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(FONT_SIZE, 1);
        switch (size) {
            case 0:
                return context.getResources().getInteger(R.integer.text_size_small);
            case 2:
                return context.getResources().getInteger(R.integer.text_size_large);
            case 1:
            default:
                return context.getResources().getInteger(R.integer.text_size_medium);
        }
    }

    public static File getTickersFile() {
        final File dir = Environment.getExternalStoragePublicDirectory("StockTickers");
        if (!dir.exists()) {
            dir.mkdir();
        }
        final String fileName = "Tickers.txt";
        final File file = new File(dir, fileName);
        return file;
    }

    public static boolean isNetworkOnline(Context context) {
        try {
            final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo i = connectivityManager.getActiveNetworkInfo();
            if (i == null)
                return false;
            if (!i.isConnected())
                return false;
            if (!i.isAvailable())
                return false;
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Takes care of weekends and afterhours
     *
     * @return
     */
    public static long getMsToNextAlarm(Context context) {
        final SharedPreferences preferences = context.getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);

        final int hourOfDay = DateTime.now().hourOfDay().get();
        final int minuteOfHour = DateTime.now().minuteOfHour().get();
        final int dayOfWeek = DateTime.now().getDayOfWeek();
        final MutableDateTime mutableDateTime = new MutableDateTime(DateTime.now());
        mutableDateTime.setZone(DateTimeZone.getDefault());

        boolean set = false;

        if (hourOfDay > 16 || (hourOfDay == 16 && minuteOfHour > 30)) { // 4:30pm
            mutableDateTime.addDays(1);
            mutableDateTime.setHourOfDay(9); // 9am
            mutableDateTime.setMinuteOfHour(35); // update at 9:45am
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
            set = true;
            mutableDateTime.setHourOfDay(9); // 9am
            mutableDateTime.setMinuteOfHour(35); // update at 9:35am
        }
        final int updatePref = preferences.getInt(UPDATE_INTERVAL, 1);
        final long time = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (updatePref + 1);
        if (set) {
            final long msToNextSchedule = mutableDateTime.getMillis() - DateTime.now().getMillis();
            return SystemClock.elapsedRealtime() + msToNextSchedule;
        } else {
            return SystemClock.elapsedRealtime() + time;
        }
    }

    public static String toCommaSeparatedString(List<String> list) {
        final StringBuilder builder = new StringBuilder();
        for (String string : list) {
            builder.append(string);
            builder.append(",");
        }
        builder.deleteCharAt(builder.length() - 1);
        return builder.toString();
    }
}
