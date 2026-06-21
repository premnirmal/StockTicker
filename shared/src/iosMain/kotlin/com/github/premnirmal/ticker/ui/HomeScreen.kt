package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_news
import com.github.premnirmal.shared.resources.ic_search
import com.github.premnirmal.shared.resources.ic_settings
import com.github.premnirmal.shared.resources.ic_trending_up
import com.github.premnirmal.shared.resources.ic_widget
import com.github.premnirmal.ticker.navigation.BottomNavigationBar
import com.github.premnirmal.ticker.navigation.HomeBottomNavDestination
import com.github.premnirmal.ticker.navigation.HomeNavHost
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.HomeScaffold
import org.jetbrains.compose.resources.painterResource

/**
 * iOS home screen hosting the shared multiplatform navigation chrome.
 *
 * It binds the shared [HomeScaffold] + [BottomNavigationBar] + [HomeNavHost] to a Compose
 * Multiplatform [androidx.navigation.NavHostController], using the shared drawable resources for the
 * tab icons. The Watchlist tab renders the existing shared [WatchlistScreen]; the remaining tabs are
 * lightweight placeholders for now. Later Phase 5 steps swap these placeholders for the full shared
 * screens once their view models can be resolved on iOS, and eventually host the complete
 * `RootNavigationGraph`.
 */
@Composable
fun HomeScreen() {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    val destinations = listOf(
        HomeBottomNavDestination(
            route = HomeRoute.Watchlist,
            selectedIcon = painterResource(Res.drawable.ic_trending_up),
            unselectedIcon = painterResource(Res.drawable.ic_trending_up),
            label = "Watchlist"
        ),
        HomeBottomNavDestination(
            route = HomeRoute.Trending,
            selectedIcon = painterResource(Res.drawable.ic_news),
            unselectedIcon = painterResource(Res.drawable.ic_news),
            label = "Trending"
        ),
        HomeBottomNavDestination(
            route = HomeRoute.Search,
            selectedIcon = painterResource(Res.drawable.ic_search),
            unselectedIcon = painterResource(Res.drawable.ic_search),
            label = "Search"
        ),
        HomeBottomNavDestination(
            route = HomeRoute.Widgets,
            selectedIcon = painterResource(Res.drawable.ic_widget),
            unselectedIcon = painterResource(Res.drawable.ic_widget),
            label = "Widgets"
        ),
        HomeBottomNavDestination(
            route = HomeRoute.Settings,
            selectedIcon = painterResource(Res.drawable.ic_settings),
            unselectedIcon = painterResource(Res.drawable.ic_settings),
            label = "Settings"
        )
    )

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = navBackStackEntry?.destination?.route ?: HomeRoute.Watchlist.route

    HomeScaffold(
        navigationType = NavigationType.BOTTOM_NAVIGATION,
        selectedDestination = selectedDestination,
        destinations = destinations,
        navigationContentPosition = NavigationContentPosition.TOP,
        snackbarHostState = snackbarHostState,
        navigateToTopLevelDestination = { destination ->
            navController.navigate(destination.route.route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
                restoreState = true
            }
        },
        navHost = { modifier ->
            HomeNavHost(
                navController = navController,
                modifier = modifier,
                watchlist = { WatchlistScreen() },
                trending = { PlaceholderTab("Trending") },
                search = { PlaceholderTab("Search") },
                widgets = { PlaceholderTab("Widgets") },
                settings = { PlaceholderTab("Settings") }
            )
        }
    )
}

@Composable
private fun PlaceholderTab(title: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text(text = title, style = MaterialTheme.typography.titleLarge)
    }
}
