package com.github.premnirmal.ticker;

import android.content.Context;

import com.github.premnirmal.ticker.network.ApiModule;

import dagger.Module;
import dagger.Provides;

/**
 * Created by premnirmal on 12/21/14.
 */
@Module(
        includes = {
                ApiModule.class
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
}
