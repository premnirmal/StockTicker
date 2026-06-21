package com.github.premnirmal.ticker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Multiplatform home NavHost. Each tab's content is supplied as a composable slot by the platform
 * host, keeping this graph free of Koin, Android resources, and platform view-model wiring.
 */
@Composable
fun HomeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    watchlist: @Composable () -> Unit,
    trending: @Composable () -> Unit,
    search: @Composable () -> Unit,
    widgets: @Composable () -> Unit,
    settings: @Composable () -> Unit,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = HomeRoute.Watchlist.route,
    ) {
        composable(HomeRoute.Watchlist.route) { watchlist() }
        composable(HomeRoute.Trending.route) { trending() }
        composable(HomeRoute.Search.route) { search() }
        composable(HomeRoute.Widgets.route) { widgets() }
        composable(HomeRoute.Settings.route) { settings() }
    }
}
