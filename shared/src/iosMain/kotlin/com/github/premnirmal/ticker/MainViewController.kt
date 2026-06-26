package com.github.premnirmal.ticker

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.ktor3.KtorNetworkFetcherFactory
import com.github.premnirmal.ticker.ui.HomeScreen
import com.github.premnirmal.ticker.ui.IosAppTheme
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import platform.UIKit.UIViewController

private object MainKoin : KoinComponent {
    val userPreferences: UserPreferences by inject()
}

/**
 * Compose Multiplatform entry point for the iOS app. Wraps the shared Compose UI in a
 * [UIViewController] that the SwiftUI shell hosts (via a `UIViewControllerRepresentable`). Exposed to
 * Swift as `MainViewControllerKt.MainViewController()`.
 *
 * The active [SelectedTheme] is observed from the shared [UserPreferences] `themePrefFlow`, so the
 * theme chosen in the Settings tab is applied live (Light/Dark/Follow system).
 *
 * Koin must already be started (see `IosModuleKt.doInitKoinIos` in `StockTickerApp.swift`) so the
 * shared screens can resolve their dependencies from the graph.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    setSingletonImageLoaderFactory { context ->
        ImageLoader.Builder(context)
            .components {
                add(KtorNetworkFetcherFactory())
            }
            .build()
    }
    val prefs = remember { MainKoin.userPreferences }
    val themePref by prefs.themePrefFlow.collectAsState(initial = prefs.themePref)
    val theme = when (themePref) {
        UserPreferences.LIGHT_THEME -> SelectedTheme.LIGHT
        UserPreferences.DARK_THEME -> SelectedTheme.DARK
        else -> SelectedTheme.SYSTEM
    }
    IosAppTheme(theme = theme) {
        HomeScreen()
    }
}
