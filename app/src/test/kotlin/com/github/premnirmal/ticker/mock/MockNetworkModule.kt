package com.github.premnirmal.ticker.mock

import android.content.Context
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.Robindahood
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module
class MockNetworkModule {

  @Provides @Singleton
  internal fun provideHttpClient(context: Context, bus: RxBus): OkHttpClient =
      Mocker.provide(OkHttpClient::class.java)

  @Provides @Singleton
  internal fun provideStocksApi(): StocksApi = Mocker.provide(StocksApi::class.java)

  @Provides @Singleton
  internal fun provideGson(): Gson = GsonBuilder().create()

  @Provides @Singleton
  internal fun provideGsonFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Provides @Singleton
  internal fun provideRxJavaFactory(): RxJava2CallAdapterFactory {
    return RxJava2CallAdapterFactory.create()
  }

  @Provides @Singleton
  internal fun provideRobindahood(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJava2CallAdapterFactory): Robindahood =
      Mocker.provide(Robindahood::class.java)

  @Provides @Singleton
  internal fun provideSuggestionsApi(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJava2CallAdapterFactory): SuggestionApi =
      Mocker.provide(SuggestionApi::class.java)

  @Provides @Singleton
  internal fun provideStocksProvider(): IStocksProvider =
      Mocker.provide(IStocksProvider::class.java)

  @Provides @Singleton
  internal fun provideWidgetDataFactory(): WidgetDataProvider =
      Mocker.provide(WidgetDataProvider::class.java)
}