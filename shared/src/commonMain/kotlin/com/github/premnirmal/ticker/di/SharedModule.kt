package com.github.premnirmal.ticker.di

import com.github.premnirmal.ticker.model.HistoryProvider
import com.github.premnirmal.ticker.network.CommitsProvider
import com.github.premnirmal.ticker.network.NewsProvider
import com.github.premnirmal.ticker.network.StocksApi
import com.github.premnirmal.ticker.network.SuggestionsProvider
import com.github.premnirmal.ticker.settings.PortfolioSerializer
import org.koin.dsl.module

/**
 * Koin module for the platform-agnostic services that already live in `:shared` `commonMain`
 * (orchestrators built from plain constructors). Their leaf dependencies — the Ktor/HTTP API
 * clients, the [com.github.premnirmal.ticker.network.CrumbStore], the [kotlinx.serialization.json.Json]
 * instance and the application [kotlinx.coroutines.CoroutineScope] — are contributed by the platform
 * module (Android wires them in `:app`; iOS wires its own equivalents in `iosMain`), so this single
 * module is reused by every platform.
 */
val sharedModule = module {
    single { CommitsProvider() }
    single {
        StocksApi(
            yahooFinanceInitialLoad = get(),
            yahooFinanceCrumb = get(),
            yahooFinance = get(),
            crumbStore = get(),
            suggestionApi = get()
        )
    }
    single {
        NewsProvider(
            coroutineScope = get(),
            googleNewsApi = get(),
            yahooNewsApi = get(),
            apeWisdom = get(),
            yahooFinanceMostActive = get(),
            stocksApi = get()
        )
    }
    single { HistoryProvider(chartApi = get()) }
    single { SuggestionsProvider(stocksApi = get()) }
    single { PortfolioSerializer(json = get()) }
}
