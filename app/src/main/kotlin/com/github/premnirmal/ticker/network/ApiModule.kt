package com.github.premnirmal.ticker.network

import android.content.Context
import android.content.SharedPreferences
import com.github.premnirmal.ticker.RxBus
import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.model.IHistoryProvider
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.StocksProvider
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
import retrofit2.adapter.rxjava.RxJavaCallAdapterFactory
import retrofit2.converter.gson.GsonConverterFactory
import java.lang.reflect.Type
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created on 3/3/16.
 */
@Module
class ApiModule {

  lateinit var stocksApi: StocksApi
  lateinit var suggestionApi: SuggestionApi


  companion object {
    internal val CONNECTION_TIMEOUT: Long = 20000
    internal val READ_TIMEOUT: Long = 20000
  }

  @Provides @Singleton
  internal fun provideHttpClient(context: Context, bus: RxBus): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient = OkHttpClient.Builder()
        .addInterceptor(NoCacheRequestInterceptor(context, bus))
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Provides @Singleton
  internal fun provideStocksApi(gson: Gson, yahooFinance: YahooFinance,
      googleFinance: GoogleFinance): StocksApi {
    stocksApi = StocksApi(gson, yahooFinance, googleFinance)
    return stocksApi
  }

  @Provides @Singleton
  internal fun provideYahooFinance(context: Context, okHttpClient: OkHttpClient,
      converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): YahooFinance {
    val Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.yahoo_endpoint))
        .addCallAdapterFactory(rxJavaFactory)
        .addConverterFactory(converterFactory)
        .build()
    val yahooFinance = Retrofit.create(YahooFinance::class.java)
    return yahooFinance
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
  internal fun provideGoogleFinance(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): GoogleFinance {
    val Retrofit: Retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.google_endpoint))
        .addCallAdapterFactory(rxJavaFactory)
        .addConverterFactory(object : Converter.Factory() {
          override fun responseBodyConverter(type: Type?, annotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<ResponseBody, *>? {
            return GStockConverter(gson)
          }

          override fun requestBodyConverter(type: Type?,
              parameterAnnotations: Array<out Annotation>?,
              methodAnnotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<*, RequestBody> {
            return converterFactory.requestBodyConverter(type, parameterAnnotations,
                methodAnnotations, retrofit)
          }

          override fun stringConverter(type: Type?, annotations: Array<out Annotation>?,
              retrofit: Retrofit?): Converter<*, String>? {
            return converterFactory.stringConverter(type, annotations, retrofit)
          }
        }).build()
    val googleFinance: GoogleFinance = Retrofit.create(GoogleFinance::class.java)
    return googleFinance
  }

  @Provides @Singleton
  internal fun provideSuggestionsApi(context: Context, okHttpClient: OkHttpClient,
      gson: Gson, converterFactory: GsonConverterFactory,
      rxJavaFactory: RxJavaCallAdapterFactory): SuggestionApi {
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
    suggestionApi = Retrofit.create(SuggestionApi::class.java)
    return suggestionApi
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
