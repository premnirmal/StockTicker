package com.github.premnirmal.ticker;

import android.content.Context;

/**
 * Created by premnirmal on 4/15/15.
 */
public class Analytics {

    public static final String SCHEDULE_UPDATE_ACTION = "ScheduleUpdate";

    private static Analytics INSTANCE;

    private Analytics(Context context) {

    }

    static void init(Context context) {
        INSTANCE = new Analytics(context);
    }

    public static void trackUpdate(String action, String label) {

    }

    public static void trackWidgetUpdate(String action) {

    }

    public static void trackWidgetSizeUpdate(String value) {

    }

    public static void trackUI(String action, String label) {

    }

    public static void trackIntialSettings(String action, String label) {

    }

    public static void trackSettingsChange(String action, String label) {

    }

}
