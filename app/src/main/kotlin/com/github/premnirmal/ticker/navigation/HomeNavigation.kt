package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.window.layout.DisplayFeature
import com.github.premnirmal.ticker.home.WatchlistScreen
import com.github.premnirmal.ticker.navigation.LayoutType.CONTENT
import com.github.premnirmal.ticker.navigation.LayoutType.HEADER
import com.github.premnirmal.ticker.news.NewsFeedScreen
import com.github.premnirmal.ticker.portfolio.search.SearchScreen
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.EmptyComingSoon
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationContentPosition.CENTER
import com.github.premnirmal.ticker.ui.NavigationContentPosition.TOP
import com.github.premnirmal.ticker.widget.WidgetsScreen
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string

@Composable
fun HomeNavHost(
  rootNavController: NavHostController,
  widthSizeClass: WindowWidthSizeClass,
  navController: NavHostController,
  displayFeatures: List<DisplayFeature>,
  modifier: Modifier = Modifier,
  contentType: ContentType
) {
  NavHost(
      modifier = modifier,
      navController = navController,
      startDestination = HomeRoute.Watchlist,
  ) {
    composable(HomeRoute.Watchlist) {
      WatchlistScreen(
          rootNavController = rootNavController,
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface),
          widthSizeClass = widthSizeClass,
          displayFeatures = displayFeatures,
          contentType = contentType
      )
    }
    composable(HomeRoute.Trending) {
      NewsFeedScreen(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface),
          onQuoteClick = {
            rootNavController.navigate("${Graph.QUOTE_DETAIL}/${it.symbol}") {
              popUpTo(HomeRoute.Trending) {
                inclusive = true
              }
            }
          }
      )
    }
    composable(HomeRoute.Search) {
      SearchScreen(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface),
          widthSizeClass = widthSizeClass,
          displayFeatures = displayFeatures,
          onQuoteClick = {
            rootNavController.navigate("${Graph.QUOTE_DETAIL}/${it.symbol}") {
              popUpTo(HomeRoute.Search) {
                inclusive = true
              }
            }
          }
      )
    }
    composable(HomeRoute.Widgets) {
      WidgetsScreen(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface),
          widthSizeClass = widthSizeClass,
          displayFeatures = displayFeatures
      )
    }
    composable(HomeRoute.Settings) {
      EmptyComingSoon(
          modifier = Modifier
              .fillMaxSize()
              .background(MaterialTheme.colorScheme.surface)
      )
    }
  }
}

@Composable
fun BottomNavigationBar(
  selectedDestination: String,
  destinations: List<HomeBottomNavDestination>,
  navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit
) {
  NavigationBar(modifier = Modifier.fillMaxWidth()) {
    destinations.forEach { destination ->
      NavigationBarItem(
          selected = selectedDestination == destination.route,
          onClick = { navigateToTopLevelDestination(destination) },
          enabled = destination.enabled,
          label = {
            Text(
                stringResource(id = destination.iconTextId),
                color = MaterialTheme.colorScheme.primary
            )
          },
          alwaysShowLabel = false,
          icon = {
            Icon(
                imageVector = destination.selectedIcon,
                contentDescription = stringResource(id = destination.iconTextId),
                tint = if (!destination.enabled) MaterialTheme.colorScheme.primary.copy(
                    alpha = 0.2f
                ) else MaterialTheme.colorScheme.primary
            )
          }
      )
    }
  }
}

@Composable
fun HomeNavigationRail(
  selectedDestination: String,
  destinations: List<HomeBottomNavDestination>,
  navigationContentPosition: NavigationContentPosition,
  navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit
) {
  NavigationRail(
      modifier = Modifier.fillMaxHeight(),
      containerColor = MaterialTheme.colorScheme.inverseOnSurface
  ) {
    // TODO remove custom nav rail positioning when NavRail component supports it
    Layout(
        modifier = Modifier.widthIn(max = 80.dp),
        content = {
          Column(
              modifier = Modifier.layoutId(HEADER),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Spacer(Modifier.height(8.dp)) // NavigationRailHeaderPadding
            Spacer(Modifier.height(4.dp)) // NavigationRailVerticalPadding
          }

          Column(
              modifier = Modifier.layoutId(CONTENT),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            destinations.forEach { Destination ->
              NavigationRailItem(
                  selected = selectedDestination == Destination.route,
                  onClick = { navigateToTopLevelDestination(Destination) },
                  enabled = Destination.enabled,
                  icon = {
                    Icon(
                        imageVector = Destination.selectedIcon,
                        contentDescription = stringResource(
                            id = Destination.iconTextId
                        ),
                        tint = if (!Destination.enabled) LocalContentColor.current.copy(
                            alpha = 0.2f
                        ) else LocalContentColor.current
                    )
                  }
              )
            }
          }
        },
        measurePolicy = { measurables, constraints ->
          lateinit var headerMeasurable: Measurable
          lateinit var contentMeasurable: Measurable
          measurables.forEach {
            when (it.layoutId) {
              HEADER -> headerMeasurable = it
              CONTENT -> contentMeasurable = it
              else -> error("Unknown layoutId encountered!")
            }
          }

          val headerPlaceable = headerMeasurable.measure(constraints)
          val contentPlaceable = contentMeasurable.measure(
              constraints.offset(vertical = -headerPlaceable.height)
          )
          layout(constraints.maxWidth, constraints.maxHeight) {
            // Place the header, this goes at the top
            headerPlaceable.placeRelative(0, 0)

            // Determine how much space is not taken up by the content
            val nonContentVerticalSpace = constraints.maxHeight - contentPlaceable.height

            val contentPlaceableY = when (navigationContentPosition) {
              // Figure out the place we want to place the content, with respect to the
              // parent (ignoring the header for now)
              TOP -> 0
              CENTER -> nonContentVerticalSpace / 2
            }
                // And finally, make sure we don't overlap with the header.
                .coerceAtLeast(headerPlaceable.height)

            contentPlaceable.placeRelative(0, contentPlaceableY)
          }
        }
    )
  }
}

class HomeNavigationActions(private val navController: NavHostController) {

  fun navigateTo(destination: HomeBottomNavDestination) {
    navController.navigate(destination.route) {
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
  }
}

object HomeRoute {
  const val Watchlist = "Watchlist"
  const val Trending = "Trending"
  const val Search = "Search"
  const val Widgets = "Widgets"
  const val Settings = "Settings"
}

data class HomeBottomNavDestination(
  val route: String,
  val selectedIcon: ImageVector,
  val unselectedIcon: ImageVector,
  val iconTextId: Int,
  val enabled: Boolean = true
)

@Preview(device = Devices.NEXUS_9)
@Composable
fun NavigationRailPreview() {
  HomeNavigationRail(
      selectedDestination = HomeRoute.Watchlist,
      destinations = listOf(
          HomeBottomNavDestination(
              HomeRoute.Watchlist,
              ImageVector.vectorResource(id = drawable.ic_trending_up),
              ImageVector.vectorResource(id = drawable.ic_trending_up),
              string.action_portfolio,
          ),
          HomeBottomNavDestination(
              HomeRoute.Trending,
              ImageVector.vectorResource(id = drawable.ic_news),
              ImageVector.vectorResource(id = drawable.ic_news),
              string.action_feed
          ),
          HomeBottomNavDestination(
              HomeRoute.Search,
              ImageVector.vectorResource(id = drawable.ic_search),
              ImageVector.vectorResource(id = drawable.ic_search),
              string.action_search
          ),
          HomeBottomNavDestination(
              HomeRoute.Widgets,
              ImageVector.vectorResource(id = drawable.ic_widget),
              ImageVector.vectorResource(id = drawable.ic_widget),
              string.action_widgets
          ),
          HomeBottomNavDestination(
              HomeRoute.Settings,
              ImageVector.vectorResource(id = drawable.ic_settings),
              ImageVector.vectorResource(id = drawable.ic_settings),
              string.action_settings
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
      selectedDestination = HomeRoute.Watchlist,
      destinations = listOf(
          HomeBottomNavDestination(
              HomeRoute.Watchlist,
              ImageVector.vectorResource(id = drawable.ic_trending_up),
              ImageVector.vectorResource(id = drawable.ic_trending_up),
              string.action_portfolio
          ),
          HomeBottomNavDestination(
              HomeRoute.Trending,
              ImageVector.vectorResource(id = drawable.ic_news),
              ImageVector.vectorResource(id = drawable.ic_news),
              string.action_feed
          ),
          HomeBottomNavDestination(
              HomeRoute.Search,
              ImageVector.vectorResource(id = drawable.ic_search),
              ImageVector.vectorResource(id = drawable.ic_search),
              string.action_search
          ),
          HomeBottomNavDestination(
              HomeRoute.Widgets,
              ImageVector.vectorResource(id = drawable.ic_widget),
              ImageVector.vectorResource(id = drawable.ic_widget),
              string.action_widgets
          ),
          HomeBottomNavDestination(
              HomeRoute.Settings,
              ImageVector.vectorResource(id = drawable.ic_settings),
              ImageVector.vectorResource(id = drawable.ic_settings),
              string.action_settings
          )
      ),
      navigateToTopLevelDestination = {}
  )
}