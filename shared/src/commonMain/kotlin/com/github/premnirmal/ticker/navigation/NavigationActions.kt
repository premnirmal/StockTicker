package com.github.premnirmal.ticker.navigation

import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController

/**
 * Encapsulates navigation actions for the home tabs (navigate-to-tab, scroll-to-top on reselect).
 * The [onNavigated] callback lets the platform host run side effects (e.g. PromptRate) after each
 * navigation.
 */
class HomeNavigationActions(
    private val navController: NavHostController,
    private val viewModel: NavigationViewModel,
    private val onNavigated: () -> Unit,
) {

    fun navigateTo(destination: HomeBottomNavDestination) {
        navController.navigate(destination.route.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
        navController.currentBackStackEntry?.let {
            if (it.destination.route == destination.route.route) {
                viewModel.scrollToTop(destination.route)
            }
        }
        onNavigated()
    }
}
