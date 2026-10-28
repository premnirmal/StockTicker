package com.github.premnirmal.ticker.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.Composable
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read

/**
 * Multiplatform root navigation graph. The home content and quote-detail content are supplied as
 * composable slots by the platform host, keeping this graph free of Android-only dependencies.
 *
 * When [disableTransitions] is true the default slide/fade navigation animation is suppressed. This
 * is used on wide layouts (e.g. iPad with a side navigation rail), where sliding the whole window in
 * from the edge looks out of place next to the static rail.
 */
@Composable
fun RootNavigationGraph(
    navHostController: NavHostController,
    homeContent: @Composable () -> Unit,
    quoteDetailContent: @Composable (symbol: String) -> Unit,
    disableTransitions: Boolean = false,
) {
    val noEnter: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition?)? =
        if (disableTransitions) ({ EnterTransition.None }) else null
    val noExit: (AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition?)? =
        if (disableTransitions) ({ ExitTransition.None }) else null
    NavHost(
        navController = navHostController,
        route = Graph.ROOT,
        startDestination = Graph.HOME
    ) {
        composable(
            route = "${Graph.QUOTE_DETAIL}/{symbol}",
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) { backStackEntry ->
            val symbol = backStackEntry.arguments?.read { getStringOrNull("symbol") }
            symbol?.let {
                quoteDetailContent(it)
            }
        }
        composable(
            route = Graph.HOME,
            enterTransition = noEnter,
            exitTransition = noExit,
            popEnterTransition = noEnter,
            popExitTransition = noExit,
        ) {
            homeContent()
        }
    }
}
