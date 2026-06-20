package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import org.koin.androidx.compose.koinViewModel
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.home.HomeEvent
import com.github.premnirmal.ticker.home.HomeViewModel
import com.github.premnirmal.ticker.home.WatchlistScreen
import com.github.premnirmal.ticker.news.NewsFeedScreen
import com.github.premnirmal.ticker.portfolio.search.AddSymbolDialog
import com.github.premnirmal.ticker.portfolio.search.SearchScreen
import com.github.premnirmal.ticker.settings.SettingsScreen
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.LocalContentType
import com.github.premnirmal.ticker.ui.NavigationContentPosition.TOP
import com.github.premnirmal.ticker.widget.WidgetsScreen
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import java.net.URLEncoder

@Composable
fun HomeNavHost(
    rootNavController: NavHostController,
    widthSizeClass: WindowWidthSizeClass,
    navController: NavHostController,
    displayFeatures: List<DisplayFeature>,
    modifier: Modifier = Modifier,
) {
    val contentType = LocalContentType.current
    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = HomeRoute.Watchlist.route,
    ) {
        composable(HomeRoute.Watchlist.route) { backStackEntry ->
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
        }
        composable(HomeRoute.Trending.route) {
            val context = LocalContext.current
            val primaryColor = MaterialTheme.colorScheme.primary
            NewsFeedScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                title = stringResource(string.news_feed),
                errorText = stringResource(string.error_fetching_news),
                holdingsLabel = stringResource(string.holdings),
                dayChangeLabel = stringResource(string.day_change_amount),
                changePercentLabel = stringResource(string.change_percent),
                gainLabel = stringResource(string.gain),
                lossLabel = stringResource(string.loss),
                changeAmountLabel = stringResource(string.change_amount),
                onArticleClick = { article ->
                    CustomTabs.openTab(context, article.url, primaryColor.toArgb())
                },
                onQuoteClick = {
                    rootNavController.navigate("${Graph.QUOTE_DETAIL}/${URLEncoder.encode(it.symbol)}") {
                        popUpTo(HomeRoute.Trending.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(HomeRoute.Search.route) {
            val context = LocalContext.current
            val primaryColor = MaterialTheme.colorScheme.primary
            val appMessaging = LocalAppMessaging.current
            val suggestionsErrorText = stringResource(string.error_fetching_suggestions)
            SearchScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                contentType = contentType,
                titleText = stringResource(string.action_search),
                searchLabel = stringResource(string.enter_a_symbol),
                noDataText = stringResource(string.no_data),
                suggestionsErrorText = suggestionsErrorText,
                holdingsLabel = stringResource(string.holdings),
                dayChangeLabel = stringResource(string.day_change_amount),
                changePercentLabel = stringResource(string.change_percent),
                gainLabel = stringResource(string.gain),
                lossLabel = stringResource(string.loss),
                changeAmountLabel = stringResource(string.change_amount),
                clearIcon = painterResource(drawable.ic_close),
                suggestionAddIcon = painterResource(drawable.ic_add_to_list),
                onArticleClick = { article ->
                    CustomTabs.openTab(context, article.url, primaryColor.toArgb())
                },
                onSuggestionsError = {
                    appMessaging.sendSnackbar(suggestionsErrorText)
                },
                twoPane = { first, second ->
                    TwoPane(
                        strategy = HorizontalTwoPaneStrategy(
                            splitFraction = 1f / 2f,
                        ),
                        displayFeatures = displayFeatures,
                        foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                        first = first,
                        second = second,
                    )
                },
                addSymbolDialog = { symbol, onDismissRequest ->
                    AddSymbolDialog(
                        symbol = symbol,
                        onDismissRequest = onDismissRequest,
                    )
                },
                onQuoteClick = {
                    rootNavController.navigate("${Graph.QUOTE_DETAIL}/${URLEncoder.encode(it.symbol)}") {
                        popUpTo(HomeRoute.Search.route) {
                            inclusive = true
                        }
                    }
                }
            )
        }
        composable(HomeRoute.Widgets.route) {
            WidgetsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                widthSizeClass = widthSizeClass,
                displayFeatures = displayFeatures
            )
        }
        composable(HomeRoute.Settings.route) { backStackEntry ->
            val homeViewModel = koinViewModel<HomeViewModel>(viewModelStoreOwner = viewModelStoreOwner)
            SettingsScreen(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.surface),
                homeViewModel = homeViewModel,
            )
        }
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


@Preview(device = Devices.NEXUS_9)
@Composable
fun NavigationRailPreview() {
    HomeNavigationRail(
        selectedDestination = HomeRoute.Watchlist.route,
        destinations = listOf(
            HomeBottomNavDestination(
                HomeRoute.Watchlist,
                ImageVector.vectorResource(id = drawable.ic_trending_up),
                ImageVector.vectorResource(id = drawable.ic_trending_up),
                label = stringResource(string.action_portfolio),
            ),
            HomeBottomNavDestination(
                HomeRoute.Trending,
                ImageVector.vectorResource(id = drawable.ic_news),
                ImageVector.vectorResource(id = drawable.ic_news),
                label = stringResource(string.action_feed)
            ),
            HomeBottomNavDestination(
                HomeRoute.Search,
                ImageVector.vectorResource(id = drawable.ic_search),
                ImageVector.vectorResource(id = drawable.ic_search),
                label = stringResource(string.action_search)
            ),
            HomeBottomNavDestination(
                HomeRoute.Widgets,
                ImageVector.vectorResource(id = drawable.ic_widget),
                ImageVector.vectorResource(id = drawable.ic_widget),
                label = stringResource(string.action_widgets)
            ),
            HomeBottomNavDestination(
                HomeRoute.Settings,
                ImageVector.vectorResource(id = drawable.ic_settings),
                ImageVector.vectorResource(id = drawable.ic_settings),
                label = stringResource(string.action_settings)
            )
        ),
        navigateToTopLevelDestination = {},
        navigationContentPosition = TOP
    )
}

@Preview
@Composable
fun BottomNavigationBarPreview() {
    BottomNavigationBar(
        selectedDestination = HomeRoute.Watchlist.route,
        destinations = listOf(
            HomeBottomNavDestination(
                HomeRoute.Watchlist,
                ImageVector.vectorResource(id = drawable.ic_trending_up),
                ImageVector.vectorResource(id = drawable.ic_trending_up),
                label = stringResource(string.action_portfolio)
            ),
            HomeBottomNavDestination(
                HomeRoute.Trending,
                ImageVector.vectorResource(id = drawable.ic_news),
                ImageVector.vectorResource(id = drawable.ic_news),
                label = stringResource(string.action_feed)
            ),
            HomeBottomNavDestination(
                HomeRoute.Search,
                ImageVector.vectorResource(id = drawable.ic_search),
                ImageVector.vectorResource(id = drawable.ic_search),
                label = stringResource(string.action_search)
            ),
            HomeBottomNavDestination(
                HomeRoute.Widgets,
                ImageVector.vectorResource(id = drawable.ic_widget),
                ImageVector.vectorResource(id = drawable.ic_widget),
                label = stringResource(string.action_widgets)
            ),
            HomeBottomNavDestination(
                HomeRoute.Settings,
                ImageVector.vectorResource(id = drawable.ic_settings),
                ImageVector.vectorResource(id = drawable.ic_settings),
                label = stringResource(string.action_settings)
            )
        ),
        navigateToTopLevelDestination = {}
    )
}
