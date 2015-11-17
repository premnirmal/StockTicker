package com.github.premnirmal.ticker;

import android.support.multidex.MultiDexApplication;

import com.github.premnirmal.tickerwidget.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by premnirmal on 11/17/15.
 */
public abstract class BaseApp extends MultiDexApplication {

    private static BaseApp instance;

    public static BaseApp getInstance() {
        return instance;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                .setDefaultFontPath("fonts/Montserrat-Regular.otf")
                .setFontAttrId(R.attr.fontPath)
                .build());
    }
}
