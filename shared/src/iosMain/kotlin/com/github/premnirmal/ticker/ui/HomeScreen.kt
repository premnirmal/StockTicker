package com.github.premnirmal.ticker.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.backhandler.BackHandler
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
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
import com.github.premnirmal.ticker.navigation.HomeNavigationActions
import com.github.premnirmal.ticker.navigation.HomeRoute
import com.github.premnirmal.ticker.navigation.HomeScaffold
import com.github.premnirmal.ticker.navigation.LocalNavGraphViewModelStoreOwner
import com.github.premnirmal.ticker.navigation.NavigationViewModel
import com.github.premnirmal.ticker.navigation.RootNavigationGraph
import com.github.premnirmal.ticker.model.IStocksProvider
// import com.github.premnirmal.ticker.review.AppReviewPrompter
import org.jetbrains.compose.resources.painterResource
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

private object HomeKoin : KoinComponent {
    val stocksProvider: IStocksProvider by inject()
    // val appReviewPrompter: AppReviewPrompter by inject()
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
    // Disabled for now — the rating prompter is intentionally not enabled on iOS yet.
    // val rootBackStackEntry by rootNavController.currentBackStackEntryAsState()
    // LaunchedEffect(rootBackStackEntry?.destination?.route) {
    //     if (rootBackStackEntry?.destination?.route?.startsWith(Graph.QUOTE_DETAIL) == true) {
    //         HomeKoin.appReviewPrompter.maybeRequestReview()
    //     }
    // }
    RootNavigationGraph(
        navHostController = rootNavController,
        disableTransitions = isWideLayout(),
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

/**
 * True when the current window is wide enough to use the dual-pane / navigation-rail layout (iPad or
 * a wide Split View). Used to suppress the root navigation slide animation, which looks out of place
 * next to the static side navigation rail.
 */
@Composable
private fun isWideLayout(): Boolean {
    val windowInfo = LocalWindowInfo.current
    val density = LocalDensity.current
    val widthDp = with(density) { windowInfo.containerSize.width.toDp() }
    return iosContentAndNavigationType(widthDp).second == ContentType.DUAL_PANE
}

@Composable
private fun HomeContent(
    rootNavController: NavHostController,
    onboardingController: OnboardingController,
) {
    val navController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }
    val whatsNewController = rememberWhatsNewController()

    val viewModelStoreOwner = checkNotNull(LocalViewModelStoreOwner.current) {
        "No ViewModelStoreOwner was provided via LocalViewModelStoreOwner"
    }
    val navigationViewModel = viewModel<NavigationViewModel>(viewModelStoreOwner) {
        NavigationViewModel()
    }
    val homeNavigationActions = remember(navController, navigationViewModel) {
        HomeNavigationActions(navController, navigationViewModel) {}
    }

    LaunchedEffect(Unit) {
        // Mirror Android's HomeViewModel.checkShowWhatsNew(): present the changelog automatically on
        // the first launch after the app has been updated, recording the installed build version.
        whatsNewController.checkShowOnLaunch(iosVersionCode())
    }

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

    CompositionLocalProvider(LocalNavGraphViewModelStoreOwner provides viewModelStoreOwner) {
        BoxWithConstraints {
            // Drive navigation/content type off the measured width so iPad (and Split View / Slide
            // Over) gets the navigation rail and the dual-pane watchlist list/detail, while iPhone
            // (and a narrow Split View) keeps the bottom bar and full-screen detail navigation.
            val (navigationType, contentType) =
                iosContentAndNavigationType(maxWidth)
            val navigationContentPosition = if (navigationType == NavigationType.NAVIGATION_RAIL) {
                NavigationContentPosition.CENTER
            } else {
                NavigationContentPosition.TOP
            }
            HomeScaffold(
                navigationType = navigationType,
                selectedDestination = selectedDestination,
                destinations = destinations,
                navigationContentPosition = navigationContentPosition,
                snackbarHostState = snackbarHostState,
                navigateToTopLevelDestination = { destination ->
                    homeNavigationActions.navigateTo(destination)
                },
                navHost = { modifier ->
                    HomeNavHost(
                        navController = navController,
                        modifier = modifier,
                        disableTransitions = navigationType == NavigationType.NAVIGATION_RAIL,
                        watchlist = {
                            WatchlistPane(
                                contentType = contentType,
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
                    settings = {
                        SettingsScreen(
                            onWhatsNew = { whatsNewController.show(iosVersionCode()) },
                            onTutorial = { onboardingController.show() },
                        )
                    }
                )
            }
            )
        }
    }
    WhatsNewBottomSheet(controller = whatsNewController, versionName = iosVersionName())
}

/**
 * The Watchlist home tab. On a compact window it renders the plain [WatchlistScreen] and navigates to
 * a full-screen quote detail through [onQuoteClick] (the iPhone behaviour). On a wide window
 * ([ContentType.DUAL_PANE], i.e. iPad / wide Split View) it shows the shared [ListDetail] with the
 * watchlist on the left and the selected quote's detail on the right — mirroring Android's
 * `WatchlistScreen` list/detail. The detail pane forces `SINGLE_PANE` so its own internal layout
 * stays a single column.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
private fun WatchlistPane(
    contentType: ContentType,
    onQuoteClick: (com.github.premnirmal.ticker.network.data.Quote) -> Unit,
) {
    if (contentType != ContentType.DUAL_PANE) {
        WatchlistScreen(onQuoteClick = onQuoteClick)
        return
    }

    // Track only the selected symbol (a saveable String) rather than the whole Quote so the
    // selection survives configuration changes without requiring Quote to be saveable.
    var selectedSymbol by rememberSaveable { mutableStateOf<String?>(null) }
    var isDetailOpen by rememberSaveable { mutableStateOf(true) }

    ListDetail(
        isDetailOpen = isDetailOpen,
        setIsDetailOpen = {
            if (!it) {
                selectedSymbol = null
            }
            isDetailOpen = it
        },
        showListAndDetail = true,
        detailKey = selectedSymbol ?: "",
        splitFraction = 1f / 2.25f,
        list = {
            WatchlistScreen(
                onQuoteClick = { quote ->
                    selectedSymbol = quote.symbol
                    isDetailOpen = true
                },
                showHeaderBackground = false,
            )
        },
        detail = {
            val symbol = selectedSymbol
            if (symbol != null) {
                QuoteDetailScreen(
                    symbol = symbol,
                    onBack = {
                        selectedSymbol = null
                        isDetailOpen = false
                    },
                    contentType = ContentType.SINGLE_PANE
                )
            } else {
                EmptyState(text = "Select a stock from your watchlist")
            }
        },
        backHandler = { onBack -> BackHandler { onBack() } },
    )
}
