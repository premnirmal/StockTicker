package com.github.premnirmal.ticker;

import android.app.Application;

import dagger.ObjectGraph;

/**
 * Created by premnirmal on 12/21/14.
 */
public class StocksApp extends Application {

    private ObjectGraph objectGraph;

    @Override
    public void onCreate() {
        super.onCreate();
        objectGraph = ObjectGraph.create(new AppModule(this));
    }

    public <T> T get(Class<T> clazz) {
        return objectGraph.get(clazz);
    }

    public void inject(Object object) {
        objectGraph.inject(object);
    }

}
