package com.github.premnirmal.ticker.mock

import android.content.Context
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.HistoricalDataApi
import com.github.premnirmal.ticker.network.NewsApi
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionApi
import com.github.premnirmal.ticker.network.YahooFinance
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/22/17.
 */
@Module
class MockNetworkModule {

  @Provides @Singleton internal fun provideHttpClient(context: Context, bus: AsyncBus): OkHttpClient =
    Mocker.provide(OkHttpClient::class)

  @Provides @Singleton internal fun provideStocksApi(): StocksApi = Mocker.provide(StocksApi::class)

  @Provides @Singleton internal fun provideGson(): Gson = GsonBuilder().create()

  @Provides @Singleton internal fun provideGsonFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Provides @Singleton internal fun provideYahooFinance(context: Context, okHttpClient: OkHttpClient,
    gson: Gson, converterFactory: GsonConverterFactory): YahooFinance = Mocker.provide(YahooFinance::class)

  @Provides @Singleton internal fun provideSuggestionsApi(context: Context,
    okHttpClient: OkHttpClient, gson: Gson, converterFactory: GsonConverterFactory): SuggestionApi = Mocker.provide(SuggestionApi::class)

  @Provides @Singleton internal fun provideStocksProvider(): IStocksProvider =
    Mocker.provide(IStocksProvider::class)

  @Provides @Singleton internal fun provideWidgetDataFactory(): WidgetDataProvider =
    Mocker.provide(WidgetDataProvider::class)

  @Provides @Singleton internal fun provideNewsApi(context: Context, okHttpClient: OkHttpClient,
    gson: Gson, converterFactory: GsonConverterFactory): NewsApi = Mocker.provide(NewsApi::class)

  @Provides @Singleton internal fun provideNewsProvider(): NewsProvider =
    Mocker.provide(NewsProvider::class)

  @Provides @Singleton internal fun provideHistoricalDataApi(context: Context,
    okHttpClient: OkHttpClient, gson: Gson, converterFactory: GsonConverterFactory): HistoricalDataApi =
    Mocker.provide(HistoricalDataApi::class)

  @Provides @Singleton internal fun provideHistoricalDataProvider(): IHistoryProvider =
    Mocker.provide(IHistoryProvider::class)

  @Provides @Singleton internal fun provideAlarmScheduler(): AlarmScheduler = AlarmScheduler()
}