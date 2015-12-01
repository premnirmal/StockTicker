package com.github.premnirmal.ticker;

import android.support.multidex.MultiDexApplication;

import com.github.premnirmal.tickerwidget.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApp extends BaseApp {

    @Override
    public void onCreate() {
        super.onCreate();
        Injector.init(this);
    }

}
