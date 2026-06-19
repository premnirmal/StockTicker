package com.github.premnirmal.ticker.components

import com.github.premnirmal.ticker.debug.DbViewerViewModel
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.news.NewsFeedViewModel
import com.github.premnirmal.ticker.news.QuoteDetailViewModel
import com.github.premnirmal.ticker.portfolio.AddPositionViewModel
import com.github.premnirmal.ticker.portfolio.AlertsViewModel
import com.github.premnirmal.ticker.portfolio.DisplaynameViewModel
import com.github.premnirmal.ticker.portfolio.NotesViewModel
import com.github.premnirmal.ticker.portfolio.search.SearchViewModel
import com.github.premnirmal.ticker.portfolio.search.SuggestionViewModel
import com.github.premnirmal.ticker.settings.SettingsViewModel
import com.github.premnirmal.ticker.ui.ThemeViewModel
import com.github.premnirmal.ticker.widget.WidgetsViewModel
import com.github.premnirmal.tickerwidget.BuildConfig
import org.koin.android.ext.koin.androidApplication
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin definitions for the app's ViewModels, replacing Hilt's `@HiltViewModel`. AndroidViewModels
 * receive the [android.app.Application] via [androidApplication]; [SuggestionViewModel] is created
 * with an assisted `symbol` parameter passed through `parametersOf` at the call site.
 */
val viewModelModule = module {
    viewModel { WidgetsViewModel(get(), get()) }
    viewModel {
        HomeViewModel(
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            get(),
            BuildConfig.VERSION_CODE,
            BuildConfig.VERSION_NAME
        )
    }
    viewModel { SettingsViewModel(get(), get(), get(), get()) }
    viewModel { ThemeViewModel(get()) }
    viewModel { DbViewerViewModel(androidApplication(), get(), get(), get()) }
    viewModel { SearchViewModel(get(), get(), get(), get()) }
    viewModel { DisplaynameViewModel(get(), get()) }
    viewModel { AlertsViewModel(get(), get()) }
    viewModel { NotesViewModel(get(), get()) }
    viewModel { AddPositionViewModel(get()) }
    viewModel {
        QuoteDetailViewModel(get(), get(), get(), get(), get())
    }
    viewModel { NewsFeedViewModel(get()) }
    viewModel { (symbol: String) -> SuggestionViewModel(symbol, get()) }
}
