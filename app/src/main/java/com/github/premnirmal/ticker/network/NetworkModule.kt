package com.github.premnirmal.ticker.network

import android.content.Context
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Converter
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

    @Provides @Singleton
    internal fun provideHttpClient(): OkHttpClient {
        val logger = HttpLoggingInterceptor()
        logger.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor(logger)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .build()
        return okHttpClient
    }

    @Named("yahoo")
    @Provides
    @Singleton
    internal fun provideHttpClientForYahoo(
        crumbInterceptor: CrumbInterceptor,
        cookieJar: YahooFinanceCookies
    ): OkHttpClient {
        val logger = HttpLoggingInterceptor()
        logger.level =
            if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        val okHttpClient =
            OkHttpClient.Builder()
                .addInterceptor { chain ->
                    val newRequest = chain.request()
                        .newBuilder()
                        .removeHeader("User-Agent")
                        .addHeader(
                            "User-Agent",
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/115.0.0.0 Safari/537.36"
                        )
                        .build()
                    chain.proceed(newRequest)
                }
                .addInterceptor(logger)
                .addInterceptor(crumbInterceptor)
                .cookieJar(cookieJar)
                .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
                .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
                .build()
        return okHttpClient
    }

    @Provides @Singleton
    internal fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            coerceInputValues = true
            prettyPrint = true
        }
    }

    @Provides @Singleton
    internal fun provideJsonFactory(json: Json): Converter.Factory {
        return json.asConverterFactory("application/json".toMediaType())
    }

    @Provides @Singleton
    internal fun provideSuggestionsApi(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): SuggestionApi {
        return createSuggestionApi(
            baseUrl = context.getString(R.string.suggestions_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideYahooFinance(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): YahooFinanceApi {
        return createYahooFinanceApi(
            baseUrl = context.getString(R.string.yahoo_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideYahooFinanceInitialLoad(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): YahooFinanceInitialLoadApi {
        return createYahooFinanceInitialLoadApi(
            baseUrl = context.getString(R.string.yahoo_initial_load_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideYahooFinanceCrumb(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): YahooCrumbApi {
        return createYahooCrumbApi(
            baseUrl = context.getString(R.string.yahoo_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideStocksApi(
        yahooFinanceInitialLoad: YahooFinanceInitialLoadApi,
        yahooFinanceCrumb: YahooCrumbApi,
        yahooFinance: YahooFinanceApi,
        appPreferences: com.github.premnirmal.ticker.AppPreferences,
        suggestionApi: SuggestionApi
    ): StocksApi {
        return StocksApi(
            yahooFinanceInitialLoad = yahooFinanceInitialLoad,
            yahooFinanceCrumb = yahooFinanceCrumb,
            yahooFinance = yahooFinance,
            crumbStore = appPreferences,
            suggestionApi = suggestionApi
        )
    }

    @Provides @Singleton
    internal fun provideApeWisdom(
        @ApplicationContext context: Context
    ): ApeWisdom {
        return ApeWisdom(baseUrl = context.getString(R.string.apewisdom_endpoint))
    }

    @Provides @Singleton
    internal fun provideYahooFinanceMostActive(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient,
    ): YahooFinanceMostActiveApi {
        return createYahooFinanceMostActiveApi(
            baseUrl = context.getString(R.string.yahoo_finance_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideGoogleNewsApi(
        @ApplicationContext context: Context,
        okHttpClient: OkHttpClient
    ): GoogleNewsApi {
        return createGoogleNewsApi(
            baseUrl = context.getString(R.string.google_news_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideYahooFinanceNewsApi(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): YahooFinanceNewsApi {
        return createYahooFinanceNewsApi(
            baseUrl = context.getString(R.string.yahoo_news_endpoint),
            okHttpClient = okHttpClient
        )
    }

    @Provides @Singleton
    internal fun provideNewsProvider(
        coroutineScope: kotlinx.coroutines.CoroutineScope,
        googleNewsApi: GoogleNewsApi,
        yahooNewsApi: YahooFinanceNewsApi,
        apeWisdom: ApeWisdom,
        yahooFinanceMostActive: YahooFinanceMostActiveApi,
        stocksApi: StocksApi
    ): NewsProvider {
        return NewsProvider(
            coroutineScope = coroutineScope,
            googleNewsApi = googleNewsApi,
            yahooNewsApi = yahooNewsApi,
            apeWisdom = apeWisdom,
            yahooFinanceMostActive = yahooFinanceMostActive,
            stocksApi = stocksApi
        )
    }

    @Provides @Singleton
    internal fun provideHistoricalDataApi(
        @ApplicationContext context: Context,
        @Named("yahoo") okHttpClient: OkHttpClient
    ): ChartApi {
        return createChartApi(
            baseUrl = context.getString(R.string.historical_data_endpoint),
            okHttpClient = okHttpClient
        )
    }

    // @Provides @Singleton
    // internal fun provideGithubApi(
    //     @ApplicationContext context: Context,
    //     okHttpClient: OkHttpClient,
    //     converterFactory: Converter.Factory
    // ): GithubApi {
    //     val retrofit = Retrofit.Builder()
    //         .client(okHttpClient)
    //         .baseUrl(context.getString(R.string.github_endoint))
    //         .addConverterFactory(converterFactory)
    //         .build()
    //     return retrofit.create(GithubApi::class.java)
    // }
}
