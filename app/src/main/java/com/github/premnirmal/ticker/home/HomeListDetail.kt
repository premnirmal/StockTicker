package com.github.premnirmal.ticker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.navigation.BottomNavigationBar
import com.github.premnirmal.ticker.navigation.HomeBottomNavDestination
import com.github.premnirmal.ticker.navigation.HomeNavHost
import com.github.premnirmal.ticker.navigation.HomeNavigationActions
import com.github.premnirmal.ticker.navigation.HomeNavigationRail
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.LocalNavGraphViewModelStoreOwner
import com.github.premnirmal.ticker.navigation.NavigationViewModel
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string

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
        val viewModel: HomeViewModel = hiltViewModel(viewModelStoreOwner)
        val hasWidget = viewModel.hasWidget.collectAsState(initial = false)
        val destinations = ArrayList<HomeBottomNavDestination>().apply {
            add(
                HomeBottomNavDestination(
                    HomeRoute.Watchlist,
                    ImageVector.vectorResource(id = drawable.ic_trending_up),
                    ImageVector.vectorResource(id = drawable.ic_trending_up),
                    string.action_portfolio
                )
            )
            if (widthSizeClass != WindowWidthSizeClass.Expanded) {
                add(
                    HomeBottomNavDestination(
                        HomeRoute.Trending,
                        ImageVector.vectorResource(id = drawable.ic_news),
                        ImageVector.vectorResource(id = drawable.ic_news),
                        string.action_feed
                    )
                )
            }
            add(
                HomeBottomNavDestination(
                    HomeRoute.Search,
                    ImageVector.vectorResource(id = drawable.ic_search),
                    ImageVector.vectorResource(id = drawable.ic_search),
                    string.action_search
                )
            )
            add(
                HomeBottomNavDestination(
                    HomeRoute.Widgets,
                    ImageVector.vectorResource(id = drawable.ic_widget),
                    ImageVector.vectorResource(id = drawable.ic_widget),
                    string.action_widgets,
                    enabled = hasWidget.value
                )
            )
            add(
                HomeBottomNavDestination(
                    HomeRoute.Settings,
                    ImageVector.vectorResource(id = drawable.ic_settings),
                    ImageVector.vectorResource(id = drawable.ic_settings),
                    string.action_settings
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeListDetailNavigationWrapper(
    modifier: Modifier = Modifier,
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
    val homeViewModel = viewModel<HomeViewModel>(viewModelStoreOwner)
    val navController = rememberNavController()
    val homeNavigationActions = remember(navController) {
        HomeNavigationActions(navController, navigationViewModel, homeViewModel)
    }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val selectedDestination = navBackStackEntry?.destination?.route ?: HomeRoute.Watchlist.route

    if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface),
            bottomBar = {
                BottomNavigationBar(
                    selectedDestination = selectedDestination,
                    navigateToTopLevelDestination = {
                        homeNavigationActions.navigateTo(it)
                    },
                    destinations = destinations
                )
            }
        ) { padding ->
            HomeNavHost(
                rootNavController = rootNavController,
                navController = navController,
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures,
                modifier = Modifier.padding(bottom = padding.calculateBottomPadding()),
            )
        }
    } else {
        Row(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            HomeNavigationRail(
                selectedDestination = selectedDestination,
                navigationContentPosition = navigationContentPosition,
                navigateToTopLevelDestination = {
                    homeNavigationActions.navigateTo(it)
                },
                destinations = destinations
            )
            HomeNavHost(
                rootNavController = rootNavController,
                navController = navController,
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

@Preview
@Composable
fun HomeListDetailHandset() {
    HomeListDetail(
        rootNavController = rememberNavController(),
        windowWidthSizeClass = WindowWidthSizeClass.Compact,
        windowHeightSizeClass = WindowHeightSizeClass.Compact,
        displayFeatures = emptyList()
    )
}

@Preview(
    device = Devices.NEXUS_9
)
@Composable
fun HomeListDetailTablet() {
    HomeListDetail(
        rootNavController = rememberNavController(),
        windowWidthSizeClass = WindowWidthSizeClass.Expanded,
        windowHeightSizeClass = WindowHeightSizeClass.Expanded,
        displayFeatures = emptyList()
    )
}
