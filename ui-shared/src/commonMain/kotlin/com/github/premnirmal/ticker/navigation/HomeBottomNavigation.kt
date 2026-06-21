package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.github.premnirmal.ticker.navigation.LayoutType.CONTENT
import com.github.premnirmal.ticker.navigation.LayoutType.HEADER
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationContentPosition.CENTER
import com.github.premnirmal.ticker.ui.NavigationContentPosition.TOP

enum class LayoutType {
    HEADER,
    CONTENT
}

/**
 * A top-level destination shown in the home bottom navigation / navigation rail. Lives in
 * `commonMain` so Android and iOS render the same bars. The icon label is a plain [String] (the
 * Android `@StringRes` lookup stays at the `:app` construction sites), keeping this model free of
 * platform resources.
 */
data class HomeBottomNavDestination(
    val route: HomeRoute,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector,
    val label: String,
    val enabled: Boolean = true
)

@Composable
fun BottomNavigationBar(
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
) {
    NavigationBar(
        modifier = Modifier,
        containerColor = MaterialTheme.colorScheme.surface,
    ) {
        destinations.forEach { destination ->
            NavigationBarItem(
                selected = selectedDestination == destination.route.route,
                onClick = { navigateToTopLevelDestination(destination) },
                enabled = destination.enabled,
                label = {
                    Text(
                        destination.label,
                        color = MaterialTheme.colorScheme.primary,
                    )
                },
                alwaysShowLabel = false,
                icon = {
                    Icon(
                        imageVector = destination.selectedIcon,
                        contentDescription = destination.label,
                        tint = if (!destination.enabled) {
                            MaterialTheme.colorScheme.primary.copy(
                                alpha = 0.2f
                            )
                        } else {
                            MaterialTheme.colorScheme.primary
                        }
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
        containerColor = MaterialTheme.colorScheme.surface
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
                    destinations.forEach { destination ->
                        NavigationRailItem(
                            selected = selectedDestination == destination.route.route,
                            onClick = { navigateToTopLevelDestination(destination) },
                            enabled = destination.enabled,
                            icon = {
                                Icon(
                                    imageVector = destination.selectedIcon,
                                    contentDescription = destination.label,
                                    tint = if (!destination.enabled) {
                                        LocalContentColor.current.copy(
                                            alpha = 0.2f
                                        )
                                    } else {
                                        LocalContentColor.current
                                    }
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
