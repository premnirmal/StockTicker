package com.github.premnirmal.ticker;

import android.support.multidex.MultiDexApplication;

import com.crashlytics.android.Crashlytics;
import com.github.premnirmal.tickerwidget.R;

import io.fabric.sdk.android.Fabric;
import uk.co.chrisjenx.calligraphy.CalligraphyConfig;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApp extends MultiDexApplication {

    @Override
    public void onCreate() {
        super.onCreate();
        Injector.init(this);
        Fabric.with(this, new Crashlytics());
        Analytics.init(this);
        CalligraphyConfig.initDefault(new CalligraphyConfig.Builder()
                        .setDefaultFontPath("fonts/Montserrat-Regular.otf")
                        .setFontAttrId(R.attr.fontPath)
                        .build()
        );
    }

}
