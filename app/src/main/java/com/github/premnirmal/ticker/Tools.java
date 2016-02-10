package com.github.premnirmal.ticker;

import android.app.AlarmManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import com.github.premnirmal.ticker.model.StocksProvider;
import com.github.premnirmal.tickerwidget.R;
import com.github.premnirmal.ticker.network.Stock;

import java.io.File;
import java.text.DecimalFormat;
import java.text.Format;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by premnirmal on 12/22/14.
 */
public final class Tools {

    public enum ChangeType {
        value, percent
    }

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
    public static final String PERCENT = "PERCENT";

    public static final Format DECIMAL_FORMAT = new DecimalFormat("0.00");

    private final SharedPreferences sharedPreferences;

    private static Tools INSTANCE;

    private static Tools getInstance() {
        if(INSTANCE == null) {
            final SharedPreferences sharedPreferences = StocksApp.getInstance().getSharedPreferences(Tools.PREFS_NAME, Context.MODE_PRIVATE);
            INSTANCE = new Tools(sharedPreferences);
            trackInitial(StocksApp.getInstance());
        }
        return INSTANCE;
    }

    private Tools(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    private static void trackInitial(Context context) {
        Analytics.trackIntialSettings(LAYOUT_TYPE, stockViewLayout() == 0 ? "Animated" : "Tabbed");
        Analytics.trackIntialSettings(TEXT_COLOR, getInstance().sharedPreferences.getInt(TEXT_COLOR, 0) == 0 ? "White" : "Dark");
        Analytics.trackIntialSettings(START_TIME, getInstance().sharedPreferences.getString(START_TIME, "09:30"));
        Analytics.trackIntialSettings(END_TIME, getInstance().sharedPreferences.getString(END_TIME, "09:30"));
        Analytics.trackIntialSettings(SETTING_AUTOSORT, Boolean.toString(autoSortEnabled()));
        Analytics.trackIntialSettings(WIDGET_BG, getInstance().sharedPreferences
                .getInt(WIDGET_BG, TRANSPARENT) + "");
        Analytics.trackIntialSettings(FONT_SIZE, getFontSize(context) + "");
        {
            final int updatePref = getInstance().sharedPreferences.getInt(Tools.UPDATE_INTERVAL, 1);
            final long time = AlarmManager.INTERVAL_FIFTEEN_MINUTES * (updatePref + 1);
            Analytics.trackIntialSettings(UPDATE_INTERVAL, time + "");
        }
        Analytics.trackIntialSettings(BOLD_CHANGE, Boolean.toString(boldEnabled()));
        final String tickerListVars = getInstance().sharedPreferences.getString(StocksProvider.SORTED_STOCK_LIST, "EMPTY!");
    }

    public static ChangeType getChangeType() {
        final boolean state = getInstance().sharedPreferences.getBoolean(PERCENT, false);
        return state ? ChangeType.percent : ChangeType.value;
    }

    static void flipChange() {
        final boolean state = getInstance().sharedPreferences.getBoolean(PERCENT, false);
        getInstance().sharedPreferences.edit().putBoolean(PERCENT, !state).apply();
    }

    public static int stockViewLayout() {
        final int pref = getInstance().sharedPreferences.getInt(LAYOUT_TYPE, 0);
        if(pref == 0) {
            return R.layout.stockview;
        } else if(pref == 1) {
            return R.layout.stockview2;
        } else {
            return R.layout.stockview3;
        }
    }

    public static int getTextColor(Context context) {
        final int pref = getInstance().sharedPreferences.getInt(TEXT_COLOR, 0);
        return pref == 0 ? Color.WHITE : context.getResources().getColor(R.color.dark_text);
    }

    public static long getUpdateInterval() {
        final int pref = getInstance().sharedPreferences.getInt(UPDATE_INTERVAL, 1);
        return AlarmManager.INTERVAL_FIFTEEN_MINUTES * (pref + 1);
    }

    public static int[] startTime() {
        final String startTimeString = getInstance().sharedPreferences.getString(START_TIME, "09:30");
        final String[] split = startTimeString.split(":");
        final int[] times = new int[]{Integer.valueOf(split[0]), Integer.valueOf(split[1])};
        return times;
    }

    public static int[] endTime() {
        final String endTimeString = getInstance().sharedPreferences.getString(END_TIME, "16:30");
        final String[] split = endTimeString.split(":");
        final int[] times = new int[]{Integer.valueOf(split[0]), Integer.valueOf(split[1])};
        return times;
    }

    public static boolean autoSortEnabled() {
        return getInstance().sharedPreferences.getBoolean(SETTING_AUTOSORT, true);
    }

    public static boolean firstTimeViewingSwipeLayout() {
        final boolean firstTime = getInstance().sharedPreferences.getBoolean(FIRST_TIME_VIEWING_SWIPELAYOUT, true);
        getInstance().sharedPreferences.edit().putBoolean(FIRST_TIME_VIEWING_SWIPELAYOUT, false).apply();
        return firstTime;
    }

    public static int getBackgroundResource(Context context) {
        final int bgPref = getInstance().sharedPreferences
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
        final int size = getInstance().sharedPreferences
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

    public static String positionsToString(List<Stock> stockList) {
        final StringBuilder builder = new StringBuilder();
        for (Stock stock : stockList) {
            if (stock.IsPosition == true){
                builder.append(stock.symbol);
                builder.append(",");
                builder.append(stock.IsPosition);
                builder.append(",");
                builder.append(stock.PositionPrice);
                builder.append(",");
                builder.append(stock.PositionShares);
                builder.append("\n");
            }
        }
        return builder.toString();
    }

    public static List<Stock> stringToPositions(String positions) {
        final ArrayList<String> tickerListCSV = new ArrayList<>(Arrays.asList(positions.split("\n")));
        ArrayList<Stock> stockList = new ArrayList<Stock>();
        ArrayList<String> tickerFields;
        Stock tmpStock;
        for (String tickerCSV : tickerListCSV) {
            tickerFields = new ArrayList<>(Arrays.asList(tickerCSV.split(",")));
            if (tickerFields.size() >= 4 && Boolean.parseBoolean(tickerFields.get(1)) == true) {
                tmpStock = new Stock();
                tmpStock.IsPosition = true;
                tmpStock.symbol = tickerFields.get(0);
                tmpStock.PositionPrice = Float.parseFloat(tickerFields.get(2));
                tmpStock.PositionShares = Float.parseFloat(tickerFields.get(3));
                stockList.add(tmpStock);
            }
        }
        return stockList;
    }

    public static boolean boldEnabled() {
        return getInstance().sharedPreferences.getBoolean(BOLD_CHANGE, false);
    }
}
