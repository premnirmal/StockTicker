package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.model.AlarmScheduler
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module
class NetworkModule {

  companion object {
    internal const val CONNECTION_TIMEOUT: Long = 5000
    internal const val READ_TIMEOUT: Long = 5000
  }

  @Provides @Singleton @Named("client") internal fun provideHttpClientForYahoo(): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level =
      if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(UserAgentInterceptor())
          .addInterceptor(logger)
          .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
          .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
          .build()
    return okHttpClient
  }

  @Provides @Singleton @Named("robindahoodClient")
  internal fun provideHttpClientForRobindahood(): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level =
      if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(RequestInterceptor())
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Provides @Singleton internal fun provideStocksApi(): StocksApi {
    return StocksApi()
  }

  @Provides @Singleton internal fun provideGson(): Gson {
    return GsonBuilder().create()
  }

  @Provides @Singleton internal fun provideGsonFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Provides @Singleton internal fun provideSuggestionsApi(
    context: Context, @Named(
        "client"
    ) okHttpClient: OkHttpClient,
    gson: Gson,
    converterFactory: GsonConverterFactory
  ): SuggestionApi {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.suggestions_endpoint))
        .addConverterFactory(object : Converter.Factory() {
          override fun responseBodyConverter(
            type: Type?,
            annotations: Array<out Annotation>?,
            retrofit: Retrofit?
          ): Converter<ResponseBody, *> {
            return StupidYahooWrapConverter(gson)
          }

          override fun requestBodyConverter(
            type: Type?,
            parameterAnnotations: Array<out Annotation>?,
            methodAnnotations: Array<out Annotation>?,
            retrofit: Retrofit?
          ): Converter<*, RequestBody>? {
            return converterFactory.requestBodyConverter(
                type, parameterAnnotations,
                methodAnnotations, retrofit
            )
          }

          override fun stringConverter(
            type: Type?,
            annotations: Array<out Annotation>?,
            retrofit: Retrofit?
          ): Converter<*, String>? {
            return converterFactory.stringConverter(type, annotations, retrofit)
          }
        })
        .build()
    val suggestionApi = retrofit.create(SuggestionApi::class.java)
    return suggestionApi
  }

  @Provides @Singleton internal fun provideRobindahood(
    context: Context, @Named(
        "robindahoodClient"
    ) okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): Robindahood {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.robindahood_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    val robindahood = retrofit.create(Robindahood::class.java)
    return robindahood
  }

  @Provides @Singleton internal fun provideYahooFinance(
    context: Context,
    @Named("client") okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): YahooFinance {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.yahoo_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    val yahooFinance = retrofit.create(YahooFinance::class.java)
    return yahooFinance
  }

  @Provides @Singleton internal fun provideNewsApi(
    context: Context, @Named(
        "robindahoodClient"
    ) okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): NewsApi {
    val retrofit =
      Retrofit.Builder()
          .client(okHttpClient)
          .baseUrl(context.getString(R.string.robindahood_endpoint))
          .addConverterFactory(converterFactory)
          .build()
    val newsApi = retrofit.create(NewsApi::class.java)
    return newsApi
  }

  @Provides @Singleton internal fun provideHistoricalDataApi(
    context: Context, @Named(
        "client"
    ) okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): HistoricalDataApi {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.alpha_vantage_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    val api = retrofit.create(HistoricalDataApi::class.java)
    return api
  }

  @Provides @Singleton internal fun provideNewsProvider(): NewsProvider = NewsProvider()

  @Provides @Singleton internal fun provideStocksProvider(): IStocksProvider = StocksProvider()

  @Provides @Singleton internal fun provideHistoricalDataProvider(): IHistoryProvider =
    HistoryProvider()

  @Provides @Singleton internal fun provideAlarmScheduler(): AlarmScheduler = AlarmScheduler()

  @Provides @Singleton internal fun provideWidgetDataFactory(): WidgetDataProvider =
    WidgetDataProvider()

}
