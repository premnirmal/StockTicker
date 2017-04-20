package com.github.premnirmal.ticker.mock

import android.content.Context
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Robindahood
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.YahooFinance
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module
class MockNetworkModule {

  @Provides @Singleton
  internal fun provideHttpClient(context: Context, bus: RxBus): OkHttpClient {
    return Mocker.provide(OkHttpClient::class.java)
  }

  @Provides @Singleton
  internal fun provideStocksApi(): StocksApi {
    return Mocker.provide(StocksApi::class.java)
  }

  @Provides @Singleton
  internal fun provideYahooFinance(context: Context,
      okHttpClient: OkHttpClient,
      converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): YahooFinance {
    return Mocker.provide(YahooFinance::class.java)
  }

  @Provides @Singleton
  internal fun provideGson(): Gson {
    return GsonBuilder().create()
  }

  @Provides @Singleton
  internal fun provideGsonFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Provides @Singleton
  internal fun provideRxJavaFactory(): RxJavaCallAdapterFactory {
    return RxJavaCallAdapterFactory.create()
  }

  @Provides @Singleton
  internal fun provideRobindahood(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): Robindahood {
    return Mocker.provide(Robindahood::class.java)
  }

  @Provides @Singleton
  internal fun provideSuggestionsApi(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): SuggestionApi {
    return Mocker.provide(SuggestionApi::class.java)
  }

  @Provides @Singleton
  internal fun provideStocksProvider(): IStocksProvider {
    return Mocker.provide(IStocksProvider::class.java)
  }

  @Provides @Singleton
  internal fun provideHistoryProvider(): IHistoryProvider {
    return Mocker.provide(IHistoryProvider::class.java)
  }

}