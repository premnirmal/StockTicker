package com.github.premnirmal.ticker.network

import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.qualifier.named
import org.koin.dsl.module
import java.util.concurrent.TimeUnit

internal const val CONNECTION_TIMEOUT: Long = 5000
internal const val READ_TIMEOUT: Long = 5000

/** Qualifier for the Yahoo-authenticated OkHttp stack (crumb + cookies + User-Agent). */
val YAHOO = named("yahoo")

/**
 * Android networking graph. Replaces the former Hilt `NetworkModule`: builds the OkHttp clients
 * and the per-endpoint API clients. The orchestrators that consume these clients (`StocksApi`,
 * `NewsProvider`, `HistoryProvider`, `CommitsProvider`) live in the shared
 * [com.github.premnirmal.ticker.di.sharedModule].
 */
val networkModule = module {
    single {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
        OkHttpClient.Builder()
            .addInterceptor(logger)
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    single(YAHOO) {
        val logger = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }
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
            .addInterceptor(get<CrumbInterceptor>())
            .cookieJar(get<YahooFinanceCookies>())
            .readTimeout(READ_TIMEOUT, TimeUnit.MILLISECONDS)
            .connectTimeout(CONNECTION_TIMEOUT, TimeUnit.MILLISECONDS)
            .build()
    }

    single { CrumbInterceptor(get()) }
    single { YahooFinanceCookies() }

    single {
        Json {
            ignoreUnknownKeys = true
            isLenient = true
            explicitNulls = false
            coerceInputValues = true
            prettyPrint = true
        }
    }

    single {
        createSuggestionApi(
            baseUrl = androidContext().getString(R.string.suggestions_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single {
        createYahooFinanceApi(
            baseUrl = androidContext().getString(R.string.yahoo_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single {
        createYahooFinanceInitialLoadApi(
            baseUrl = androidContext().getString(R.string.yahoo_initial_load_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single {
        createYahooCrumbApi(
            baseUrl = androidContext().getString(R.string.yahoo_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single { ApeWisdom(baseUrl = androidContext().getString(R.string.apewisdom_endpoint)) }
    single {
        createYahooFinanceMostActiveApi(
            baseUrl = androidContext().getString(R.string.yahoo_finance_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single {
        createGoogleNewsApi(
            baseUrl = androidContext().getString(R.string.google_news_endpoint),
            okHttpClient = get()
        )
    }
    single {
        createYahooFinanceNewsApi(
            baseUrl = androidContext().getString(R.string.yahoo_news_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
    single {
        createChartApi(
            baseUrl = androidContext().getString(R.string.historical_data_endpoint),
            okHttpClient = get(YAHOO)
        )
    }
}
