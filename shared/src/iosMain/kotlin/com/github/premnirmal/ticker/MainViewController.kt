package com.github.premnirmal.ticker

import androidx.compose.ui.window.ComposeUIViewController
import com.github.premnirmal.ticker.ui.HomeScreen
import com.github.premnirmal.ticker.ui.IosAppTheme
import platform.UIKit.UIViewController

/**
 * Compose Multiplatform entry point for the iOS app. Wraps the shared Compose UI in a
 * [UIViewController] that the SwiftUI shell hosts (via a `UIViewControllerRepresentable`). Exposed to
 * Swift as `MainViewControllerKt.MainViewController()`.
 *
 * Koin must already be started (see `IosModuleKt.doInitKoinIos` in `StockTickerApp.swift`) so the
 * shared screens can resolve their dependencies from the graph.
 */
fun MainViewController(): UIViewController = ComposeUIViewController {
    IosAppTheme {
        HomeScreen()
    }
}
