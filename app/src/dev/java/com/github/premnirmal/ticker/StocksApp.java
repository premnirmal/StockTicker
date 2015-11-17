package com.github.premnirmal.ticker;

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
