package com.github.premnirmal.ticker.ui

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.github.premnirmal.shared.resources.Res
import com.github.premnirmal.shared.resources.ic_news
import com.github.premnirmal.shared.resources.ic_search
import com.github.premnirmal.shared.resources.ic_settings
import com.github.premnirmal.shared.resources.ic_trending_up
import com.github.premnirmal.ticker.navigation.Graph
import com.github.premnirmal.ticker.navigation.HomeBottomNavDestination
import com.github.premnirmal.ticker.navigation.HomeNavHost
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.HomeScaffold
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.review.AppReviewPrompter
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object HomeKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
    val appReviewPrompter: AppReviewPrompter by inject()
}

/**
 * iOS entry screen hosting the full shared multiplatform [RootNavigationGraph].
 *
 * A root [NavHostController] drives the shared [RootNavigationGraph], whose `homeContent` slot is the
 * home navigation chrome ([HomeContent]) and whose `quoteDetailContent` slot is the iOS
 * [QuoteDetailScreen]. The home tabs navigate to the quote-detail destination through the same root
 * controller, so the iOS app shares the Android navigation structure. The Watchlist tab renders the
 * shared [WatchlistScreen]; the remaining tabs are lightweight placeholders until their view models
 * can be resolved on iOS.
 */
@Composable
fun HomeScreen() {
    val rootNavController = rememberNavController()
    val onboardingController = rememberOnboardingController()
    LaunchedEffect(Unit) {
        // Mirror Android's HomeActivity, which calls stocksProvider.schedule() on first launch to
        // enqueue the periodic background refresh + cleanup (the iOS BGTaskScheduler requests) and
        // arm the next update.
        HomeKoin.stocksProvider.schedule()
        onboardingController.showIfFirstRun()
    }
    // Mirror Android's HomeActivity: when the user opens a quote detail, ask StoreKit for an in-app
    // review (gated on UserPreferences.shouldPromptRate + a once-per-session guard inside the prompter).
    val rootBackStackEntry by rootNavController.currentBackStackEntryAsState()
    LaunchedEffect(rootBackStackEntry?.destination?.route) {
        if (rootBackStackEntry?.destination?.route?.startsWith(Graph.QUOTE_DETAIL) == true) {
            HomeKoin.appReviewPrompter.maybeRequestReview()
        }
    }
    RootNavigationGraph(
        navHostController = rootNavController,
        homeContent = { HomeContent(rootNavController, onboardingController) },
        quoteDetailContent = { symbol ->
            QuoteDetailScreen(
                symbol = symbol,
                onBack = { rootNavController.popBackStack() }
            )
        }
    )
    OnboardingTutorial(onboardingController)
}

@Composable
private fun HomeContent(
    rootNavController: NavHostController,
    onboardingController: OnboardingController,
) {
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
                watchlist = {
                    WatchlistScreen(
                        onQuoteClick = { quote ->
                            rootNavController.navigate("${Graph.QUOTE_DETAIL}/${quote.symbol}")
                        }
                    )
                },
                trending = {
                    TrendingScreen(
                        onQuoteClick = { quote ->
                            rootNavController.navigate("${Graph.QUOTE_DETAIL}/${quote.symbol}")
                        }
                    )
                },
                search = {
                    SearchScreen(
                        onQuoteClick = { quote ->
                            rootNavController.navigate("${Graph.QUOTE_DETAIL}/${quote.symbol}")
                        }
                    )
                },
                widgets = {},
                settings = { SettingsScreen(onTutorial = { onboardingController.show() }) }
            )
        }
    )
}
