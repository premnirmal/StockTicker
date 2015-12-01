package com.github.premnirmal.ticker.network;

import android.content.Context;
import android.content.SharedPreferences;

import com.github.premnirmal.ticker.RefreshReceiver;
import com.github.premnirmal.ticker.RxBus;
import com.github.premnirmal.ticker.UpdateReceiver;
import com.github.premnirmal.ticker.model.HistoryProvider;
import com.github.premnirmal.ticker.model.IHistoryProvider;
import com.github.premnirmal.ticker.model.IStocksProvider;
import com.github.premnirmal.ticker.model.StocksProvider;
import com.github.premnirmal.ticker.portfolio.AddPositionActivity;
import com.github.premnirmal.ticker.portfolio.EditPositionActivity;
import com.github.premnirmal.ticker.portfolio.GraphActivity;
import com.github.premnirmal.ticker.portfolio.PortfolioFragment;
import com.github.premnirmal.ticker.portfolio.RearrangeActivity;
import com.github.premnirmal.ticker.portfolio.TickerSelectorActivity;
import com.github.premnirmal.ticker.settings.SettingsActivity;
import com.github.premnirmal.ticker.widget.RemoteStockViewAdapter;
import com.github.premnirmal.ticker.widget.StockWidget;
import com.github.premnirmal.tickerwidget.R;

import javax.inject.Singleton;

import dagger.Module;
import dagger.Provides;
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
                UpdateReceiver.class,
                RefreshReceiver.class,
                GraphActivity.class,
                PortfolioFragment.class,
                RearrangeActivity.class,
                AddPositionActivity.class,
                EditPositionActivity.class
        },
        complete = false,
        library = true
)
public class ApiModule {

    private StocksApi stocksApi;
    private SuggestionApi suggestionApi;

    @Provides
    @Singleton
    StocksApi provideStocksApi(YahooFinance yahooFinance) {
        if (stocksApi == null) {
            stocksApi = new StocksApi(yahooFinance);
        }
        return stocksApi;
    }

    @Provides
    @Singleton
    YahooFinance provideYahooFinance(Context context) {
        final RestAdapter restAdapter = new RestAdapter.Builder()
                .setEndpoint(context.getString(R.string.yahoo_endpoint))
                .build();
        final YahooFinance yahooFinance = restAdapter.create(YahooFinance.class);
        return yahooFinance;
    }

//    @Provides
//    @Singleton
//    GoogleFinance provideGoogleFinance(Context context) {
//        final RestAdapter restAdapter = new RestAdapter.Builder()
//                .setEndpoint(context.getString(R.string.google_endpoint))
//                .setConverter(new GStockConverter())
//                .build();
//        final GoogleFinance googleFinance = restAdapter.create(GoogleFinance.class);
//        return googleFinance;
//    }

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
    IStocksProvider provideStocksProvider(Context context, StocksApi stocksApi, RxBus bus, SharedPreferences sharedPreferences) {
        return new StocksProvider(stocksApi, bus, context, sharedPreferences);
    }

    @Provides
    @Singleton
    IHistoryProvider provideHistoryProvider(Context context, StocksApi stocksApi) {
        return new HistoryProvider(stocksApi, context);
    }

}
