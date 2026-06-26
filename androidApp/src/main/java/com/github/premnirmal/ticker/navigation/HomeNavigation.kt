package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavHostController
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.home.HomeEvent
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.home.WatchlistScreen
import com.github.premnirmal.ticker.news.NewsFeedScreen
import com.github.premnirmal.ticker.portfolio.search.SearchScreen
import com.github.premnirmal.ticker.settings.SettingsScreen
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.widget.WidgetsScreen
import org.koin.androidx.compose.koinViewModel
import java.net.URLEncoder

/**
 * Android-specific host for [HomeNavHost] that supplies the concrete screen composables
 * (which require Koin, Android resources, window-size-class, etc.) as slots.
 */
@Composable
fun HomeNavHostWrapper(
    rootNavController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    navController: NavHostController,
    displayFeatures: List<DisplayFeature>,
    modifier: Modifier = Modifier,
    disableTransitions: Boolean = false,
) {
    val contentType = LocalContentType.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    HomeNavHost(
        navController = navController,
        modifier = modifier,
        disableTransitions = disableTransitions,
        watchlist = {
            val homeViewModel = koinViewModel<HomeViewModel>(viewModelStoreOwner = viewModelStoreOwner)
            WatchlistScreen(
                rootNavController = rootNavController,
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures,
                contentType = contentType,
                viewModel = homeViewModel,
            )
        },
        trending = {
            NewsFeedScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                onQuoteClick = {
                    rootNavController.navigate("${Graph.QUOTE_DETAIL}/${URLEncoder.encode(it.symbol)}") {
                        popUpTo(HomeRoute.Trending.route) {
                            inclusive = true
                        }
                    }
                }
            )
        },
        search = {
            SearchScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures,
                onQuoteClick = {
                    rootNavController.navigate("${Graph.QUOTE_DETAIL}/${URLEncoder.encode(it.symbol)}") {
                        popUpTo(HomeRoute.Search.route) {
                            inclusive = true
                        }
                    }
                }
            )
        },
        widgets = {
            WidgetsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures
            )
        },
        settings = {
            val homeViewModel = koinViewModel<HomeViewModel>(viewModelStoreOwner = viewModelStoreOwner)
            SettingsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                homeViewModel = homeViewModel,
            )
        },
    )
}

