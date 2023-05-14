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
import retrofit2.converter.scalars.ScalarsConverterFactory
import retrofit2.converter.simplexml.SimpleXmlConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
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

  @Provides @Singleton internal fun provideHttpClient(
    userAgentInterceptor: UserAgentInterceptor,
  ): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level =
      if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient =
      OkHttpClient.Builder()
        .addInterceptor(userAgentInterceptor)
        .addInterceptor(logger)
        .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
        .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
        .build()
    return okHttpClient
  }

  @Named("yahoo")
  @Provides @Singleton internal fun provideHttpClientForYahoo(
    userAgentInterceptor: UserAgentInterceptor,
    crumbInterceptor: CrumbInterceptor,
    cookieJar: YahooFinanceCookies
  ): OkHttpClient {
    val logger = HttpLoggingInterceptor()
    logger.level =
      if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
    val okHttpClient =
      OkHttpClient.Builder()
          .addInterceptor(userAgentInterceptor)
          .addInterceptor(logger)
          .addInterceptor(crumbInterceptor)
          .cookieJar(cookieJar)
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
    @Named("yahoo") okHttpClient: OkHttpClient,
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
    @Named("yahoo") okHttpClient: OkHttpClient,
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

  @Provides @Singleton internal fun provideYahooFinanceInitialLoad(
    @ApplicationContext context: Context,
    cookieJar: YahooFinanceCookies
  ): YahooFinanceInitialLoad {
    val retrofit = Retrofit.Builder()
      .client(OkHttpClient().newBuilder()
        .addInterceptor { chain ->
          val original = chain.request()
          val newRequest = original
            .newBuilder()
            .removeHeader("Accept")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .build()
          chain.proceed(newRequest)
        }
        .cookieJar(cookieJar)
        .build()
      )
      .addConverterFactory(ScalarsConverterFactory.create())
      .baseUrl(context.getString(R.string.yahoo_initial_load_endpoint))
      .build()
    val yahooFinance = retrofit.create(YahooFinanceInitialLoad::class.java)
    return yahooFinance
  }

  @Provides @Singleton internal fun provideYahooFinanceCrumb(
    @ApplicationContext context: Context,
    cookieJar: YahooFinanceCookies
  ): YahooFinanceCrumb {
    val retrofit = Retrofit.Builder()
      .client(OkHttpClient().newBuilder()
        .addInterceptor { chain ->
          val original = chain.request()
          val newRequest = original
            .newBuilder()
            .removeHeader("Accept")
            .addHeader("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .build()
          chain.proceed(newRequest)
        }
        .cookieJar(cookieJar)
        .build()
      )
      .addConverterFactory(ScalarsConverterFactory.create())
      .baseUrl(context.getString(R.string.yahoo_endpoint))
      .build()
    val yahooFinance = retrofit.create(YahooFinanceCrumb::class.java)
    return yahooFinance
  }

  @Provides @Singleton internal fun provideYahooQuoteDetailsApi(
    @ApplicationContext context: Context,
    @Named("yahoo") okHttpClient: OkHttpClient,
    converterFactory: GsonConverterFactory
  ): YahooQuoteDetails {
    val retrofit = Retrofit.Builder()
        .client(okHttpClient)
        .baseUrl(context.getString(R.string.yahoo_endpoint_quote_details))
        .addConverterFactory(converterFactory)
        .build()
    val yahooFinance = retrofit.create(YahooQuoteDetails::class.java)
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
    @Named("yahoo") okHttpClient: OkHttpClient,
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
    @Named("yahoo") okHttpClient: OkHttpClient
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
    @Named("yahoo") okHttpClient: OkHttpClient,
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
