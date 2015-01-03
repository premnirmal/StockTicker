package com.github.premnirmal.ticker;

import android.app.AlarmManager;
import android.content.Context;
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
    public static final String FONT_SIZE = "com.github.premnirmal.ticker.fontsize";
    public static final String SETTING_AUTOSORT = "SETTING_AUTOSORT";

    public static float getFontSize(Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getInt(FONT_SIZE, context.getResources().getInteger(R.integer.text_size_medium));
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
    public static long getMsToNextAlarm() {
        final int hourOfDay = DateTime.now().hourOfDay().get();
        final int dayOfWeek = DateTime.now().getDayOfWeek();
        final MutableDateTime mutableDateTime = new MutableDateTime(DateTime.now());
        mutableDateTime.setZone(DateTimeZone.getDefault());

        boolean set = false;

        if (hourOfDay >= 17) { // 5pm
            mutableDateTime.addDays(1);
            mutableDateTime.setHourOfDay(9); // 9am
            mutableDateTime.setMinuteOfHour(15); // update at 9:15am
            set = true;
        }

        if (set && dayOfWeek == DateTimeConstants.FRIDAY) {
            mutableDateTime.addDays(2);
        }

        if (dayOfWeek > DateTimeConstants.FRIDAY) {
            set = true;
            if (dayOfWeek == DateTimeConstants.SATURDAY) {
                mutableDateTime.addDays(2);
            } else if (dayOfWeek == DateTimeConstants.SUNDAY) {
                mutableDateTime.addDays(1);
            }
            mutableDateTime.setHourOfDay(9); // 9am
            mutableDateTime.setMinuteOfHour(15); // update at 9:15am
        }

        if (set) {
            final long msToNextSchedule = mutableDateTime.getMillis() - DateTime.now().getMillis();
            return SystemClock.elapsedRealtime() + msToNextSchedule;
        } else {
            return SystemClock.elapsedRealtime() + (AlarmManager.INTERVAL_FIFTEEN_MINUTES * 2);
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
