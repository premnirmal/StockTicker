package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Created by premnirmal on 3/3/16.
 */
@Module
@InstallIn(SingletonComponent::class)
class NetworkModule {

  companion object {
    internal const val CONNECTION_TIMEOUT: Long = 5000
    internal const val READ_TIMEOUT: Long = 5000
  }

  @Provides @Singleton internal fun provideHttpClientForYahoo(@ApplicationContext context: Context): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level =
      if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(UserAgentInterceptor(context))
          .addInterceptor(logger)
          .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
          .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
          .build()
    return okHttpClient
  }

  @Provides @Singleton internal fun provideGson(): Gson {
    return GsonBuilder().setLenient()
        .create()
  }

  @Provides @Singleton internal fun provideGsonFactory(gson: Gson): GsonConverterFactory {
    return GsonConverterFactory.create(gson)
  }

  @Provides @Singleton internal fun provideSuggestionsApi(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): SuggestionApi {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.suggestions_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    return retrofit.create(SuggestionApi::class.java)
  }

  @Provides @Singleton internal fun provideYahooFinance(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
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

  @Provides @Singleton internal fun provideApeWisdom(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): ApeWisdom {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.apewisdom_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    val apewisdom = retrofit.create(ApeWisdom::class.java)
    return apewisdom
  }

  @Provides @Singleton internal fun provideYahooFinanceMostActive(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
  ): YahooFinanceMostActive {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.yahoo_finance_endpoint))
        .addConverterFactory(JsoupConverterFactory())
        .build()
    return retrofit.create(YahooFinanceMostActive::class.java)
  }

  @Provides @Singleton internal fun provideGoogleNewsApi(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
  ): GoogleNewsApi {
    val retrofit =
      Retrofit.Builder()
          .client(okHttpClient)
          .baseUrl(context.getString(R.string.google_news_endpoint))
          .addConverterFactory(SimpleXmlConverterFactory.create())
          .build()
    return retrofit.create(GoogleNewsApi::class.java)
  }

  @Provides @Singleton internal fun provideYahooFinanceNewsApi(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient
  ): YahooFinanceNewsApi {
    val retrofit =
      Retrofit.Builder()
          .client(okHttpClient)
          .baseUrl(context.getString(R.string.yahoo_news_endpoint))
          .addConverterFactory(SimpleXmlConverterFactory.create())
          .build()
    return retrofit.create(YahooFinanceNewsApi::class.java)
  }

  @Provides @Singleton internal fun provideHistoricalDataApi(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): ChartApi {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.historical_data_endpoint))
        .addConverterFactory(converterFactory)
        .build()
    val api = retrofit.create(ChartApi::class.java)
    return api
  }

  @Provides @Singleton internal fun provideGithubApi(
    @ApplicationContext context: Context,
    okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): GithubApi {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.github_endoint))
        .addConverterFactory(converterFactory)
        .build()
    return retrofit.create(GithubApi::class.java)
  }
}
