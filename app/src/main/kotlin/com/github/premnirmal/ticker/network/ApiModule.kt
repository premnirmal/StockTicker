package com.github.premnirmal.ticker.network

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.tickerwidget.R
import com.squareup.okhttp.OkHttpClient
import dagger.Module
import dagger.Provides
import retrofit.RestAdapter
import retrofit.client.OkClient
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created on 3/3/16.
 */
@Module
class ApiModule {

    private var stocksApi: StocksApi? = null
    private var suggestionApi: SuggestionApi? = null


    companion object {
        internal val CONNECTION_TMEOUT: Long = 20000
        internal val READ_TMEOUT: Long = 20000
    }

    @Provides @Singleton
    internal fun provideHttpClient(): OkClient {
        val okHttpClient = OkHttpClient()
        okHttpClient.interceptors().add(NoCacheRequestInterceptor())
        okHttpClient.setConnectTimeout(CONNECTION_TMEOUT, TimeUnit.MILLISECONDS)
        okHttpClient.setReadTimeout(READ_TMEOUT, TimeUnit.MILLISECONDS)
        val client = OkClient(okHttpClient)
        return client
    }

    @Provides @Singleton
    internal fun provideStocksApi(yahooFinance: YahooFinance): StocksApi {
        if (stocksApi == null) {
            stocksApi = StocksApi(yahooFinance)
        }
        return stocksApi as StocksApi
    }

    @Provides @Singleton
    internal fun provideYahooFinance(context: Context, okHttpClient: OkClient): YahooFinance {
        val restAdapter = RestAdapter.Builder()
                .setClient(okHttpClient)
                .setEndpoint(context.getString(R.string.yahoo_endpoint))
                .build()
        val yahooFinance = restAdapter.create(YahooFinance::class.java)
        return yahooFinance
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

    @Provides @Singleton
    internal fun provideSuggestionsApi(context: Context, okHttpClient: OkClient): SuggestionApi {
        if (suggestionApi == null) {
            val restAdapter = RestAdapter.Builder()
                    .setClient(okHttpClient)
                    .setEndpoint(context.getString(R.string.suggestions_endpoint))
                    .setConverter(StupidYahooWrapConverter())
                    .build()
            suggestionApi = restAdapter.create(SuggestionApi::class.java)
        }
        return suggestionApi as SuggestionApi
    }

    @Provides @Singleton
    internal fun provideStocksProvider(context: Context, stocksApi: StocksApi, bus: RxBus, sharedPreferences: SharedPreferences): IStocksProvider {
        return StocksProvider(stocksApi, bus, context, sharedPreferences)
    }

    @Provides @Singleton
    internal fun provideHistoryProvider(context: Context, stocksApi: StocksApi): IHistoryProvider {
        return HistoryProvider(stocksApi, context)
    }

}
