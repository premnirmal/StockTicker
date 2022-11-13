package com.github.premnirmal.ticker.home

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.github.premnirmal.ticker.home.watchlist.WatchlistScreen
import com.github.premnirmal.ticker.network.data.Quote
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
import kotlinx.coroutines.flow.StateFlow

@Composable
fun HomeListDetail(
  windowSizeClass: WindowSizeClass,
  displayFeatures: List<DisplayFeature>,
  homeViewModel: HomeViewModel = viewModel()
) {
  HomeListDetail(windowSizeClass, displayFeatures, homeViewModel.portfolio)
}

@Composable
fun HomeListDetail(
  windowSizeClass: WindowSizeClass,
  displayFeatures: List<DisplayFeature>,
  quotesFlow: StateFlow<List<Quote>>
) {
  // Query for the current window size class
  val widthSizeClass by rememberUpdatedState(windowSizeClass.widthSizeClass)
  val heightSizeClass by rememberUpdatedState(windowSizeClass.heightSizeClass)

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
      navigationType = NavigationType.NAVIGATION_RAIL
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
      navigationContentPosition = navigationContentPosition,
      navigateToTopLevelDestination = { destination ->

      }
  )
}

@Composable
private fun HomeListDetailNavigationWrapper(
  modifier: Modifier = Modifier,
  navigationType: NavigationType,
  contentType: ContentType,
  displayFeatures: List<DisplayFeature>,
  navigationContentPosition: NavigationContentPosition,
  navigateToTopLevelDestination: (TopLevelDestination) -> Unit,
  quotesFlow: StateFlow<List<Quote>>
) {
  val scope = rememberCoroutineScope()
  val navController = rememberNavController()
  val navigationActions = remember(navController) {
    NavigationActions(navController)
  }
  val navBackStackEntry by navController.currentBackStackEntryAsState()
  val selectedDestination = navBackStackEntry?.destination?.route ?: Route.Watchlist
  // todo add destinations
  val destinations = emptyList<TopLevelDestination>()

  Row(modifier = modifier.fillMaxSize()) {
    AnimatedVisibility(visible = navigationType == NavigationType.NAVIGATION_RAIL) {
      HomeNavigationRail(
          selectedDestination = selectedDestination,
          navigationContentPosition = navigationContentPosition,
          navigateToTopLevelDestination = navigateToTopLevelDestination,
          destinations = destinations
      )
    }
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.inverseOnSurface)
    ) {
      HomeNavHost(
          navController = navController,
          displayFeatures = displayFeatures,
          modifier = Modifier.weight(1f),
          quotesFlow = quotesFlow,
          contentType = contentType
      )
      AnimatedVisibility(visible = navigationType == NavigationType.BOTTOM_NAVIGATION) {
        BottomNavigationBar(
            selectedDestination = selectedDestination,
            navigateToTopLevelDestination = navigateToTopLevelDestination,
            destinations = destinations
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
      EmptyComingSoon()
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


