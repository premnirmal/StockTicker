package com.github.premnirmal.ticker.mock

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.network.GoogleFinance
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.YahooFinance
import dagger.Module
import dagger.Provides
import org.mockito.Mockito.mock
import retrofit.client.OkClient
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module
class MockApiModule {

  @Provides @Singleton
  internal fun provideHttpClient(context: Context, bus: RxBus): OkClient {
    return mock(OkClient::class.java)
  }

  @Provides @Singleton
  internal fun provideStocksApi(yahooFinance: YahooFinance,
      googleFinance: GoogleFinance): StocksApi {
    return mock(StocksApi::class.java)
  }

  @Provides @Singleton
  internal fun provideYahooFinance(context: Context, okHttpClient: OkClient): YahooFinance {
    return mock(YahooFinance::class.java)
  }

  @Provides @Singleton
  internal fun provideGoogleFinance(context: Context, okHttpClient: OkClient): GoogleFinance {
    return mock(GoogleFinance::class.java)
  }

  @Provides @Singleton
  internal fun provideSuggestionsApi(context: Context, okHttpClient: OkClient): SuggestionApi {
    return mock(SuggestionApi::class.java)
  }

  @Provides @Singleton
  internal fun provideStocksProvider(context: Context, stocksApi: StocksApi, bus: RxBus,
      sharedPreferences: SharedPreferences): IStocksProvider {
    return StocksProvider(stocksApi, bus, context, sharedPreferences)
  }

  @Provides @Singleton
  internal fun provideHistoryProvider(context: Context, stocksApi: StocksApi): IHistoryProvider {
    return HistoryProvider(stocksApi, context)
  }

}