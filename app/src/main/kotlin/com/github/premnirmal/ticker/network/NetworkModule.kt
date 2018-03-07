package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.model.AlarmScheduler
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
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
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
    internal const val CONNECTION_TIMEOUT: Long = 20000
    internal const val READ_TIMEOUT: Long = 20000
  }

  @Provides @Singleton @Named("yahooClient")
  internal fun provideHttpClientForYahoo(): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Provides @Singleton @Named("newsClient")
  internal fun provideHttpClientForNews(): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Provides @Singleton @Named("robindahoodClient")
  internal fun provideHttpClientForRobindahood(context: Context, bus: RxBus): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(RequestInterceptor())
        .addInterceptor(UserAgentInterceptor())
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Provides @Singleton
  internal fun provideStocksApi(): StocksApi {
    return StocksApi()
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
  internal fun provideRxJavaFactory(): RxJava2CallAdapterFactory {
    return RxJava2CallAdapterFactory.create()
  }

  @Provides @Singleton
  internal fun provideSuggestionsApi(context: Context,
      @Named("yahooClient") okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJava2CallAdapterFactory): SuggestionApi {
    val Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.suggestions_endpoint))
        .addCallAdapterFactory(rxJavaFactory)
        .addConverterFactory(object : Converter.Factory() {
          override fun responseBodyConverter(type: Type?, annotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<ResponseBody, *> {
            return StupidYahooWrapConverter(gson)
          }

          override fun requestBodyConverter(type: Type?,
              parameterAnnotations: Array<out Annotation>?,
              methodAnnotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<*, RequestBody>? {
            return converterFactory.requestBodyConverter(type, parameterAnnotations,
                methodAnnotations, retrofit)
          }

          override fun stringConverter(type: Type?, annotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<*, String>? {
            return converterFactory.stringConverter(type, annotations, retrofit)
          }
        })
        .build()
    val suggestionApi = Retrofit.create(SuggestionApi::class.java)
    return suggestionApi
  }

  @Provides @Singleton
  internal fun provideRobindahood(context: Context,
      @Named("robindahoodClient") okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJava2CallAdapterFactory): Robindahood {
    val Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.robindahood_endpoint))
        .addCallAdapterFactory(rxJavaFactory)
        .addConverterFactory(converterFactory)
        .build()
    val robindahood = Retrofit.create(Robindahood::class.java)
    return robindahood
  }

  @Provides @Singleton
  internal fun provideNewsApi(context: Context,
      @Named("newsClient") okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJava2CallAdapterFactory): NewsApi {
    val Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.news_endpoint))
        .addCallAdapterFactory(rxJavaFactory)
        .addConverterFactory(converterFactory)
        .build()
    val newsApi = Retrofit.create(NewsApi::class.java)
    return newsApi
  }

  @Provides @Singleton
  internal fun provideNewsProvider(): NewsProvider = NewsProvider()

  @Provides @Singleton
  internal fun provideStocksProvider(): IStocksProvider = StocksProvider()

  @Provides @Singleton
  internal fun provideAlarmScheduler(): AlarmScheduler = AlarmScheduler()

  @Provides @Singleton
  internal fun provideWidgetDataFactory(): WidgetDataProvider = WidgetDataProvider()

}
