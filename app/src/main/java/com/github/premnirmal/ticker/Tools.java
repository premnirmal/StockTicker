package com.github.premnirmal.ticker;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Environment;

import java.io.File;

/**
 * Created by premnirmal on 12/22/14.
 */
public final class Tools {

    public static final String PREFS_NAME = "com.github.premnirmal.ticker";
    public static final String FONT_SIZE = "com.github.premnirmal.ticker.fontsize";

    public static String buildQuery(Object[] objects) {
        final StringBuilder commaSeparator = new StringBuilder();
        for (Object object : objects) {
            commaSeparator.append(object.toString().replace("^", "").replaceAll(" ", "").trim());
            commaSeparator.append(',');
        }
        if (objects.length > 0) {
            commaSeparator.deleteCharAt(commaSeparator.length() - 1);
        }

        return "select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(\"" + commaSeparator.toString() + "\")";
    }

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
}
