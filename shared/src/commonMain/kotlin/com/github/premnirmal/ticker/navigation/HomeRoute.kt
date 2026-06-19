package com.github.premnirmal.ticker.navigation

/**
 * The top-level destinations shown in the home bottom navigation / navigation rail. Lives in
 * `commonMain` so the shared [NavigationViewModel] (and the future Compose Multiplatform
 * navigation) can reference it on every platform.
 */
enum class HomeRoute(val route: String) {
    Watchlist("Watchlist"),
    Trending("Trending"),
    Search("Search"),
    Widgets("Widgets"),
    Settings("Settings")
}
