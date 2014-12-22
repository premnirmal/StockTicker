package com.github.premnirmal.ticker;

import android.content.Context;

/**
 * Created by premnirmal on 12/22/14.
 */
public final class Tools {

    public static final String PREFS_NAME = "com.github.premnirmal.ticker";
    public static final String FONT_SIZE = "com.github.premnirmal.ticker.fontsize";

    public static String buildQuery(Object[] objects) {
        final StringBuilder commaSeparator = new StringBuilder();
        for (Object object : objects) {
            commaSeparator.append(object.toString().replace("^", ""));
            commaSeparator.append(',');
        }
        if (objects.length > 0) {
            commaSeparator.deleteCharAt(commaSeparator.length() - 1);
        }

        return "select%20*%20from%20yahoo.finance.quotes%20where%20symbol%20in%20(\"" + commaSeparator.toString() + "\")";
    }

    public static float getFontSize(Context context) {
        return context.getSharedPreferences(PREFS_NAME,Context.MODE_PRIVATE)
                .getInt(FONT_SIZE,context.getResources().getInteger(R.integer.text_size_medium));
    }
}
