package com.github.premnirmal.ticker.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.github.premnirmal.ticker.home.HomeEvent
import com.github.premnirmal.ticker.home.HomeViewModel

/**
 * The home bottom-navigation graph, shared across Android and iOS via Compose Multiplatform
 * navigation. The graph wiring (the [HomeRoute] routes + their start destination) lives in
 * `commonMain`, while each route's screen content is supplied as a `@Composable` slot so the
 * Android-resource-coupled pieces (string/painter resources, `CustomTabs`, root-graph navigation)
 * stay at the `:app` call site (see `:app`'s `AppHomeNavHost`).
 */
@Composable
fun HomeNavHost(
    navController: NavHostController,
    watchlistContent: @Composable () -> Unit,
    trendingContent: @Composable () -> Unit,
    searchContent: @Composable () -> Unit,
    widgetsContent: @Composable () -> Unit,
    settingsContent: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = HomeRoute.Watchlist.route,
    ) {
        composable(HomeRoute.Watchlist.route) { watchlistContent() }
        composable(HomeRoute.Trending.route) { trendingContent() }
        composable(HomeRoute.Search.route) { searchContent() }
        composable(HomeRoute.Widgets.route) { widgetsContent() }
        composable(HomeRoute.Settings.route) { settingsContent() }
    }
}

class HomeNavigationActions(
    private val navController: NavHostController,
    private val viewModel: NavigationViewModel,
    private val homeViewModel: HomeViewModel,
) {

    fun navigateTo(destination: HomeBottomNavDestination) {
        navController.navigate(destination.route.route) {
            // Pop up to the start destination of the graph to
            // avoid building up a large stack of destinations
            // on the back stack as users select items
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            // Avoid multiple copies of the same destination when
            // reselecting the same item
            launchSingleTop = true
            // Restore state when reselecting a previously selected item
            restoreState = true
        }
        navController.currentBackStackEntry?.let {
            if (it.destination.route == destination.route.route) {
                viewModel.scrollToTop(destination.route)
            }
        }
        homeViewModel.sendHomeEvent(HomeEvent.PromptRate)
    }
}
