package com.github.premnirmal.ticker.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.savedstate.read

/**
 * Multiplatform root navigation graph. The home content and quote-detail content are supplied as
 * composable slots by the platform host, keeping this graph free of Android-only dependencies.
 */
@Composable
fun RootNavigationGraph(
    navHostController: NavHostController,
    homeContent: @Composable () -> Unit,
    quoteDetailContent: @Composable (symbol: String) -> Unit,
) {
    NavHost(
        navController = navHostController,
        route = Graph.ROOT,
        startDestination = Graph.HOME
    ) {
        composable(route = "${Graph.QUOTE_DETAIL}/{symbol}") { backStackEntry ->
            val symbol = backStackEntry.arguments?.read { getStringOrNull("symbol") }
            symbol?.let {
                quoteDetailContent(it)
            }
        }
        composable(route = Graph.HOME) {
            homeContent()
        }
    }
}
