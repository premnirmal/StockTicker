package com.github.premnirmal.ticker;

import android.support.multidex.MultiDexApplication;

import com.github.premnirmal.tickerwidget.R;

import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Injector.init(this);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Montserrat-Regular.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }

}
