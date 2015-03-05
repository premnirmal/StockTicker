package com.github.premnirmal.ticker;

import android.content.Context;

import com.github.premnirmal.ticker.network.ApiModule;
import com.github.premnirmal.ticker.portfolio.PortfolioFragment;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;

/**
 * Created by premnirmal on 12/21/14.
 */
@Module(
        includes = {
                ApiModule.class
        },
        injects = {
                PortfolioFragment.class
        },
        complete = true,
        library = false
)
public class AppModule {

    private final Context app;

    public AppModule(Context app) {
        this.app = app;
    }

    @Provides
    Context provideApplicationContext() {
        return app;
    }

    @Provides
    @Singleton
    RxBus provideEventBus() {
        return new RxBus();
    }
}
