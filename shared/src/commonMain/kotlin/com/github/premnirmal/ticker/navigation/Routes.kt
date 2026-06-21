package com.github.premnirmal.ticker.navigation

/**
 * Top-level navigation graph route constants.
 */
object Graph {
    const val ROOT = "root_graph"
    const val HOME = "home_graph"
    const val QUOTE_DETAIL = "quote_detail_graph"
}

/**
 * Routes for the home bottom/rail navigation destinations.
 */
enum class HomeRoute(val route: String) {
    Watchlist("Watchlist"),
    Trending("Trending"),
    Search("Search"),
    Widgets("Widgets"),
    Settings("Settings")
}
