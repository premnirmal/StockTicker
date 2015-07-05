package com.github.premnirmal.ticker;


import com.crashlytics.android.Crashlytics;

/**
 * Created by premnirmal on 7/5/15.
 */
public class CrashLogger {

    public static void logException(Throwable throwable) {
        Crashlytics.logException(throwable);
    }
}
