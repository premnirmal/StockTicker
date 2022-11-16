package com.github.premnirmal.ticker.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.github.premnirmal.ticker.home.watchlist.WatchlistScreen
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.ticker.news.NewsFeedScreen
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.DevicePosture
import com.github.premnirmal.ticker.ui.EmptyComingSoon
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.ticker.ui.isBookPosture
import com.github.premnirmal.ticker.ui.isSeparating
import com.github.premnirmal.ticker.ui.navigation.BottomNavigationBar
import com.github.premnirmal.ticker.ui.navigation.HomeNavigationRail
import com.github.premnirmal.ticker.ui.navigation.NavigationActions
import com.github.premnirmal.ticker.ui.navigation.Route
import com.github.premnirmal.ticker.ui.navigation.TopLevelDestination
import com.github.premnirmal.tickerwidget.R
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeListDetail(
  windowWidthSizeClass: WindowWidthSizeClass,
  windowHeightSizeClass: WindowHeightSizeClass,
  displayFeatures: List<DisplayFeature>,
  homeViewModel: HomeViewModel = hiltViewModel()
) {
  HomeListDetail(
      windowWidthSizeClass, windowHeightSizeClass, displayFeatures, homeViewModel.portfolio
  )
}

@Composable
fun HomeListDetail(
  windowWidthSizeClass: WindowWidthSizeClass,
  windowHeightSizeClass: WindowHeightSizeClass,
  displayFeatures: List<DisplayFeature>,
  quotesFlow: StateFlow<List<Quote>>
) {
  // Query for the current window size class
  val widthSizeClass by rememberUpdatedState(windowWidthSizeClass)
  val heightSizeClass by rememberUpdatedState(windowHeightSizeClass)

  /**
   * This will help us select type of navigation and content type depending on window size and
   * fold state of the device.
   */
  val navigationType: NavigationType
  val contentType: ContentType

  /**
   * We are using display's folding features to map the device postures a fold is in.
   * In the state of folding device If it's half fold in BookPosture we want to avoid content
   * at the crease/hinge
   */
  val foldingFeature = displayFeatures.filterIsInstance<FoldingFeature>()
      .firstOrNull()

  val foldingDevicePosture = when {
    isBookPosture(foldingFeature) ->
      DevicePosture.BookPosture(foldingFeature.bounds)

    isSeparating(foldingFeature) ->
      DevicePosture.Separating(foldingFeature.bounds, foldingFeature.orientation)

    else -> DevicePosture.NormalPosture
  }

  when (widthSizeClass) {
    WindowWidthSizeClass.Compact -> {
      navigationType = NavigationType.BOTTOM_NAVIGATION
      contentType = ContentType.SINGLE_PANE
    }
    WindowWidthSizeClass.Medium -> {
      navigationType = NavigationType.BOTTOM_NAVIGATION
      contentType = if (foldingDevicePosture != DevicePosture.NormalPosture) {
        ContentType.DUAL_PANE
      } else {
        ContentType.SINGLE_PANE
      }
    }
    WindowWidthSizeClass.Expanded -> {
      navigationType = NavigationType.NAVIGATION_RAIL
      contentType = ContentType.DUAL_PANE
    }
    else -> {
      navigationType = NavigationType.BOTTOM_NAVIGATION
      contentType = ContentType.SINGLE_PANE
    }
  }

  /**
   * Content inside Navigation Rail/Drawer can also be positioned at top, bottom or center for
   * ergonomics and reachability depending upon the height of the device.
   */
  val navigationContentPosition = when (heightSizeClass) {
    WindowHeightSizeClass.Compact -> {
      NavigationContentPosition.TOP
    }
    WindowHeightSizeClass.Medium,
    WindowHeightSizeClass.Expanded -> {
      NavigationContentPosition.CENTER
    }
    else -> {
      NavigationContentPosition.TOP
    }
  }

  HomeListDetailNavigationWrapper(
      navigationType = navigationType,
      contentType = contentType,
      displayFeatures = displayFeatures,
      quotesFlow = quotesFlow,
      navigationContentPosition = navigationContentPosition
  )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HomeListDetailNavigationWrapper(
  modifier: Modifier = Modifier,
  navigationType: NavigationType,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  navigationContentPosition: NavigationContentPosition,
  quotesFlow: StateFlow<List<Quote>>
) {
  val navController = rememberNavController()
  val navigationActions = remember(navController) {
    NavigationActions(navController)
  }
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val selectedDestination = navBackStackEntry?.destination?.route ?: Route.Watchlist
  val destinations = listOf(
      TopLevelDestination(
          Route.Watchlist,
          ImageVector.vectorResource(id = R.drawable.ic_trending_up),
          ImageVector.vectorResource(id = R.drawable.ic_trending_up),
          R.string.action_portfolio
      ),
      TopLevelDestination(
          Route.Trending,
          ImageVector.vectorResource(id = R.drawable.ic_news),
          ImageVector.vectorResource(id = R.drawable.ic_news),
          R.string.action_feed
      ),
      TopLevelDestination(
          Route.Search,
          ImageVector.vectorResource(id = R.drawable.ic_search),
          ImageVector.vectorResource(id = R.drawable.ic_search),
          R.string.action_search
      ),
      TopLevelDestination(
          Route.Widgets,
          ImageVector.vectorResource(id = R.drawable.ic_widget),
          ImageVector.vectorResource(id = R.drawable.ic_widget),
          R.string.action_widgets
      ),
      TopLevelDestination(
          Route.Settings,
          ImageVector.vectorResource(id = R.drawable.ic_settings),
          ImageVector.vectorResource(id = R.drawable.ic_settings),
          R.string.action_settings
      )
  )

  if (navigationType == NavigationType.BOTTOM_NAVIGATION) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface),
        bottomBar = {
          BottomNavigationBar(
              selectedDestination = selectedDestination,
              navigateToTopLevelDestination = {
                navigationActions.navigateTo(it)
              },
              destinations = destinations
          )
        }
    ) { padding ->
      HomeNavHost(
          navController = navController,
          displayFeatures = displayFeatures,
          modifier = Modifier.padding(padding),
          quotesFlow = quotesFlow,
          contentType = contentType
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
            navigationActions.navigateTo(it)
          },
          destinations = destinations
      )
      Column {
        HomeNavHost(
            navController = navController,
            displayFeatures = displayFeatures,
            modifier = Modifier.weight(1f),
            quotesFlow = quotesFlow,
            contentType = contentType
        )
      }
    }
  }
}

@Composable
private fun HomeNavHost(
  navController: NavHostController,
  displayFeatures: List<DisplayFeature>,
  modifier: Modifier = Modifier,
  contentType: ContentType,
  quotesFlow: StateFlow<List<Quote>>
) {
  NavHost(
      modifier = modifier,
      navController = navController,
      startDestination = Route.Watchlist,
  ) {
    composable(Route.Watchlist) {
      WatchlistScreen(
          displayFeatures = displayFeatures,
          quotesFlow = quotesFlow,
          contentType = contentType
      )
    }
    composable(Route.Trending) {
      NewsFeedScreen(onQuoteClick = {
        // TODO open quote detail
      })
    }
    composable(Route.Search) {
      EmptyComingSoon()
    }
    composable(Route.Widgets) {
      EmptyComingSoon()
    }
    composable(Route.Settings) {
      EmptyComingSoon()
    }
  }
}

@Preview
@Composable
fun HomeListDetailHandset() {
  HomeListDetail(
      windowWidthSizeClass = WindowWidthSizeClass.Compact,
      windowHeightSizeClass = WindowHeightSizeClass.Compact,
      displayFeatures = emptyList(),
      quotesFlow = MutableStateFlow(listOf(Quote("GOOG", "Alphabet Corp.", 0f, 0f, 0f)))
  )
}

@Preview(
    device = Devices.NEXUS_9
)
@Composable
fun HomeListDetailTablet() {
  HomeListDetail(
      windowWidthSizeClass = WindowWidthSizeClass.Expanded,
      windowHeightSizeClass = WindowHeightSizeClass.Expanded,
      displayFeatures = emptyList(),
      quotesFlow = MutableStateFlow(listOf(Quote("GOOG", "Alphabet Corp.", 0f, 0f, 0f)))
  )
}


