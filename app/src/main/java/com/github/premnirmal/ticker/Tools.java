package com.github.premnirmal.ticker;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import com.github.premnirmal.tickerwidget.R;

import java.io.File;
import java.util.List;

/**
 * Created by premnirmal on 12/22/14.
 */
public final class Tools {

    public static final String PREFS_NAME = "com.github.premnirmal.ticker";
    public static final String FONT_SIZE = "com.github.premnirmal.ticker.textsize";
    public static final String START_TIME = "START_TIME";
    public static final String END_TIME = "END_TIME";
    public static final String SETTING_AUTOSORT = "SETTING_AUTOSORT";
    public static final String WIDGET_BG = "WIDGET_BG";
    public static final String TEXT_COLOR = "TEXT_COLOR";
    public static final String UPDATE_INTERVAL = "UPDATE_INTERVAL";
    public static final int TRANSPARENT = 0;
    public static final int TRANSLUCENT = 1;
    public static final int DARK = 2;
    public static final int LIGHT = 3;
    public static final String LAYOUT_TYPE = "LAYOUT_TYPE";
    public static final String BOLD_CHANGE = "BOLD_CHANGE";
    public static final String FIRST_TIME_VIEWING_SWIPELAYOUT = "FIRST_TIME_VIEWING_SWIPELAYOUT";
    public static final String WHATS_NEW = "WHATS_NEW";

    private final SharedPreferences sharedPreferences;

    private static Tools INSTANCE;

    static void init(SharedPreferences sharedPreferences) {
        INSTANCE = new Tools(sharedPreferences);
    }

    private Tools(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public static int stockViewLayout() {
        final int pref = INSTANCE.sharedPreferences.getInt(LAYOUT_TYPE, 0);
        return pref == 0 ? R.layout.stockview : R.layout.stockview2;
    }

    public static int getTextColor(Context context) {
        final int pref = INSTANCE.sharedPreferences.getInt(TEXT_COLOR, 0);
        return pref == 0 ? Color.WHITE : context.getResources().getColor(R.color.dark_text);
    }

    public static int[] startTime() {
        final String startTimeString = INSTANCE.sharedPreferences.getString(START_TIME, "09:30");
        final String[] split = startTimeString.split(":");
        final int[] times = new int[]{Integer.valueOf(split[0]), Integer.valueOf(split[1])};
        return times;
    }

    public static int[] endTime() {
        final String endTimeString = INSTANCE.sharedPreferences.getString(END_TIME, "16:30");
        final String[] split = endTimeString.split(":");
        final int[] times = new int[]{Integer.valueOf(split[0]), Integer.valueOf(split[1])};
        return times;
    }

    public static boolean autoSortEnabled() {
        return INSTANCE.sharedPreferences.getBoolean(SETTING_AUTOSORT, true);
    }

    public static boolean firstTimeViewingSwipeLayout() {
        final boolean firstTime = INSTANCE.sharedPreferences.getBoolean(FIRST_TIME_VIEWING_SWIPELAYOUT, true);
        INSTANCE.sharedPreferences.edit().putBoolean(FIRST_TIME_VIEWING_SWIPELAYOUT, false).apply();
        return firstTime;
    }

    public static int getBackgroundResource(Context context) {
        final int bgPref = INSTANCE.sharedPreferences
                .getInt(WIDGET_BG, TRANSPARENT);
        switch (bgPref) {
            case TRANSLUCENT:
                return R.drawable.translucent_widget_bg;
            case DARK:
                return R.drawable.dark_widget_bg;
            case LIGHT:
                return R.drawable.light_widget_bg;
            case TRANSPARENT:
            default:
                return R.drawable.transparent_widget_bg;

        }
    }

    public static float getFontSize(Context context) {
        final int size = INSTANCE.sharedPreferences
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

    public static String toCommaSeparatedString(List<String> list) {
        final StringBuilder builder = new StringBuilder();
        for (String string : list) {
            builder.append(string);
            builder.append(",");
        }
        final int length = builder.length();
        if (length > 1) {
            builder.deleteCharAt(length - 1);
        }
        return builder.toString();
    }

    public static boolean boldEnabled() {
        return INSTANCE.sharedPreferences.getBoolean(BOLD_CHANGE, false);
    }
}
