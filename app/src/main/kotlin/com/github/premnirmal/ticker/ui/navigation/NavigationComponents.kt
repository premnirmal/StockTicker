package com.github.premnirmal.ticker.ui.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import com.github.premnirmal.ticker.ui.NavigationContentPosition

@Composable
fun BottomNavigationBar(
  selectedDestination: String,
  destinations: List<TopLevelDestination>,
  navigateToTopLevelDestination: (TopLevelDestination) -> Unit
) {
  NavigationBar(modifier = Modifier.fillMaxWidth()) {
    destinations.forEach { destination ->
      NavigationBarItem(
          selected = selectedDestination == destination.route,
          onClick = { navigateToTopLevelDestination(destination) },
          icon = {
            Icon(
                imageVector = destination.selectedIcon,
                contentDescription = stringResource(id = destination.iconTextId)
            )
          }
      )
    }
  }
}

@Composable
fun HomeNavigationRail(
  selectedDestination: String,
  destinations: List<TopLevelDestination>,
  navigationContentPosition: NavigationContentPosition,
  navigateToTopLevelDestination: (TopLevelDestination) -> Unit
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
              modifier = Modifier.layoutId(LayoutType.HEADER),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            Spacer(Modifier.height(8.dp)) // NavigationRailHeaderPadding
            Spacer(Modifier.height(4.dp)) // NavigationRailVerticalPadding
          }

          Column(
              modifier = Modifier.layoutId(LayoutType.CONTENT),
              horizontalAlignment = Alignment.CenterHorizontally,
              verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            destinations.forEach { Destination ->
              NavigationRailItem(
                  selected = selectedDestination == Destination.route,
                  onClick = { navigateToTopLevelDestination(Destination) },
                  icon = {
                    Icon(
                        imageVector = Destination.selectedIcon,
                        contentDescription = stringResource(
                            id = Destination.iconTextId
                        )
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
              LayoutType.HEADER -> headerMeasurable = it
              LayoutType.CONTENT -> contentMeasurable = it
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
              NavigationContentPosition.TOP -> 0
              NavigationContentPosition.CENTER -> nonContentVerticalSpace / 2
            }
                // And finally, make sure we don't overlap with the header.
                .coerceAtLeast(headerPlaceable.height)

            contentPlaceable.placeRelative(0, contentPlaceableY)
          }
        }
    )
  }
}

enum class LayoutType {
  HEADER, CONTENT
}

class NavigationActions(private val navController: NavHostController) {

  fun navigateTo(destination: TopLevelDestination) {
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