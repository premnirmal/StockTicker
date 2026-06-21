package com.github.premnirmal.ticker.home

import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.navigation.HomeBottomNavDestination
import com.github.premnirmal.ticker.navigation.HomeNavigationActions
import com.github.premnirmal.ticker.navigation.HomeNavHostWrapper
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.HomeScaffold
import com.github.premnirmal.ticker.navigation.LocalNavGraphViewModelStoreOwner
import com.github.premnirmal.ticker.navigation.NavigationViewModel
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeListDetail(
    rootNavController: NavHostController,
    windowWidthSizeClass: WindowWidthSizeClass,
    windowHeightSizeClass: WindowHeightSizeClass,
    displayFeatures: List<DisplayFeature>
) {
    // Query for the current window size class
    val widthSizeClass by rememberUpdatedState(windowWidthSizeClass)
    val heightSizeClass by rememberUpdatedState(windowHeightSizeClass)

    val pair = calculateContentAndNavigationType(widthSizeClass, displayFeatures)
    val navigationType = pair.first
    val contentType = pair.second

    CompositionLocalProvider(LocalContentType provides contentType) {
        /**
         * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
         * ergonomics and reachability depending upon the height of the device.
         */
        val navigationContentPosition = when (heightSizeClass) {
            WindowHeightSizeClass.Compact -> {
                NavigationContentPosition.TOP
            }

            WindowHeightSizeClass.Medium, WindowHeightSizeClass.Expanded -> {
                NavigationContentPosition.CENTER
            }

            else -> {
                NavigationContentPosition.TOP
            }
        }
        val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
            "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
        }
        val viewModel: HomeViewModel = koinViewModel(viewModelStoreOwner = viewModelStoreOwner)
        val hasWidget = viewModel.hasWidget.collectAsState(initial = false)
        val destinations = ArrayList<HomeBottomNavDestination>().apply {
            add(
                HomeBottomNavDestination(
                    HomeRoute.Watchlist,
                    selectedIcon = painterResource(id = drawable.ic_trending_up),
                    unselectedIcon = painterResource(id = drawable.ic_trending_up),
                    label = stringResource(string.action_portfolio)
                )
            )
            if (widthSizeClass != WindowWidthSizeClass.Expanded) {
                add(
                    HomeBottomNavDestination(
                        HomeRoute.Trending,
                        selectedIcon = painterResource(id = drawable.ic_news),
                        unselectedIcon = painterResource(id = drawable.ic_news),
                        label = stringResource(string.action_feed)
                    )
                )
            }
            add(
                HomeBottomNavDestination(
                    HomeRoute.Search,
                    selectedIcon = painterResource(id = drawable.ic_search),
                    unselectedIcon = painterResource(id = drawable.ic_search),
                    label = stringResource(string.action_search)
                )
            )
            add(
                HomeBottomNavDestination(
                    HomeRoute.Widgets,
                    selectedIcon = painterResource(id = drawable.ic_widget),
                    unselectedIcon = painterResource(id = drawable.ic_widget),
                    label = stringResource(string.action_widgets),
                    enabled = hasWidget.value
                )
            )
            add(
                HomeBottomNavDestination(
                    HomeRoute.Settings,
                    selectedIcon = painterResource(id = drawable.ic_settings),
                    unselectedIcon = painterResource(id = drawable.ic_settings),
                    label = stringResource(string.action_settings)
                )
            )
        }

        HomeListDetailNavigationWrapper(
            rootNavController = rootNavController,
            navigationType = navigationType,
            destinations = destinations,
            widthSizeClass = windowWidthSizeClass,
            displayFeatures = displayFeatures,
            navigationContentPosition = navigationContentPosition,
        )
    }
}

@Composable
private fun HomeListDetailNavigationWrapper(
    rootNavController: NavHostController,
    navigationType: NavigationType,
    destinations: List<HomeBottomNavDestination>,
    widthSizeClass: WindowWidthSizeClass,
    displayFeatures: List<DisplayFeature>,
    navigationContentPosition: NavigationContentPosition,
) {
    val viewModelStoreOwner = checkNotNull(LocalNavGraphViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalNavGraphViewModelStoreOwner"
    }
    val navigationViewModel = viewModel<NavigationViewModel>(viewModelStoreOwner)
    val homeViewModel = koinViewModel<HomeViewModel>(viewModelStoreOwner = viewModelStoreOwner)
    val navController = rememberNavController()
    val homeNavigationActions = remember(navController) {
        HomeNavigationActions(navController, navigationViewModel) {
            homeViewModel.sendHomeEvent(HomeEvent.PromptRate)
        }
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = navBackStackEntry?.destination?.route ?: HomeRoute.Watchlist.route

    HomeScaffold(
        navigationType = navigationType,
        selectedDestination = selectedDestination,
        destinations = destinations,
        navigationContentPosition = navigationContentPosition,
        snackbarHostState = LocalAppMessaging.current.snackbarHostState,
        navigateToTopLevelDestination = { homeNavigationActions.navigateTo(it) },
        navHost = { modifier ->
            HomeNavHostWrapper(
                rootNavController = rootNavController,
                navController = navController,
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures,
                modifier = modifier,
            )
        }
    )
}
