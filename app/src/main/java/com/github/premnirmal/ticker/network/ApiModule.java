package com.github.premnirmal.ticker.network;

import android.content.Context;

import com.github.premnirmal.ticker.R;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.model.StocksProvider;
import com.github.premnirmal.ticker.ui.ParanormalActivity;
import com.github.premnirmal.ticker.ui.SettingsActivity;
import com.github.premnirmal.ticker.ui.TickerSelectorActivity;
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter;
import com.github.premnirmal.ticker.widget.StockWidget;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
import de.greenrobot.event.EventBus;
import retrofit.RestAdapter;

/**
 * Created by premnirmal on 12/21/14.
 */

@Module(
        injects = {
                TickerSelectorActivity.class,
                RemoteStockViewAdapter.class,
                SettingsActivity.class,
                StockWidget.class,
                ParanormalActivity.class
        },
        complete = false,
        library = true
)
public class ApiModule {

    private StocksApi stocksApi;
    private SuggestionApi suggestionApi;
    private EventBus eventBus;

    @Provides
    @Singleton
    StocksApi provideStocksApi(Context context) {
        if (stocksApi == null) {
            final RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(context.getString(R.string.endpoint))
                    .build();
            stocksApi = restAdapter.create(StocksApi.class);
        }
        return stocksApi;
    }

    @Provides
    @Singleton
    SuggestionApi provideSuggestionsApi(Context context) {
        if (suggestionApi == null) {
            final RestAdapter restAdapter = new RestAdapter.Builder()
                    .setEndpoint(context.getString(R.string.suggestions_endpoint))
                    .setConverter(new StupidYahooWrapConverter())
                    .build();
            suggestionApi = restAdapter.create(SuggestionApi.class);
        }
        return suggestionApi;
    }

    @Provides
    @Singleton
    IStocksProvider provideStocksProvider(Context context) {
        return new StocksProvider(provideStocksApi(context), provideEventBus(), context);
    }


    @Provides
    @Singleton
    EventBus provideEventBus() {
        if (eventBus == null) {
            eventBus = EventBus.getDefault();
        }
        return eventBus;
    }

}
