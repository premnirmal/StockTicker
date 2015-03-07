package com.github.premnirmal.ticker;

import dagger.ObjectGraph;

/**
 * Created by premnirmal on 2/20/15.
 */
public class Injector {

    private ObjectGraph objectGraph;

    private static Injector instance;

    static void init(StocksApp app) {
        instance = new Injector(app);
    }

    private Injector(StocksApp app) {
        Tools.init(app);
        objectGraph = ObjectGraph.create(new AppModule(app));
        instance = this;
    }

    public static <T> T get(Class<T> clazz) {
        return instance.objectGraph.get(clazz);
    }

    public static void inject(Object object) {
        instance.objectGraph.inject(object);
    }

}
