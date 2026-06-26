package com.github.premnirmal.ticker.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable

/**
 * Multiplatform home NavHost. Each tab's content is supplied as a composable slot by the platform
 * host, keeping this graph free of Koin, Android resources, and platform view-model wiring.
 *
 * When [disableTransitions] is true the default slide/fade animation between tabs is suppressed.
 * This is used on wide layouts (e.g. iPad with a side navigation rail), where sliding a tab in from
 * the edge looks out of place when switching tabs from the static rail.
 */
@Composable
fun HomeNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier,
    disableTransitions: Boolean = false,
    watchlist: @Composable () -> Unit,
    trending: @Composable () -> Unit,
    search: @Composable () -> Unit,
    widgets: @Composable () -> Unit,
    settings: @Composable () -> Unit,
) {
    val noEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        if (disableTransitions) ({ EnterTransition.None }) else null
    val noExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        if (disableTransitions) ({ ExitTransition.None }) else null
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = HomeRoute.Watchlist.route,
    ) {
        composable(
            HomeRoute.Watchlist.route,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { watchlist() }
        composable(
            HomeRoute.Trending.route,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { trending() }
        composable(
            HomeRoute.Search.route,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { search() }
        composable(
            HomeRoute.Widgets.route,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { widgets() }
        composable(
            HomeRoute.Settings.route,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { settings() }
    }
}
