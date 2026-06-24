package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationContentPosition.CENTER
import com.github.premnirmal.ticker.ui.NavigationContentPosition.TOP

/** Blur radius applied to the content captured behind the glass bottom navigation bar. */
private val GlassBlurRadius = 24.dp

/** Opacity of the surface tint layered over the blurred backdrop to keep items legible. */
private const val GlassTintAlpha = 0.55f

/**
 * Layout-id enum for positioning content in the [HomeNavigationRail] custom layout.
 */
enum class LayoutType {
    HEADER,
    CONTENT
}

/**
 * Multiplatform data class describing a home bottom-navigation/rail destination. Uses resolved
 * [Painter] icons and [String] labels so that it is free of Android resource IDs.
 */
data class HomeBottomNavDestination(
    val route: HomeRoute,
    val selectedIcon: Painter,
    val unselectedIcon: Painter,
    val label: String,
    val contentDescription: String = label,
    val enabled: Boolean = true
)

/**
 * Multiplatform bottom navigation bar for the home screen.
 *
 * When a [backdrop] graphics layer is supplied (the home content captured by [HomeScaffold]), the
 * bar renders as translucent "liquid glass": the slice of content sitting behind the bar is drawn
 * blurred underneath a thin surface tint, so the bar reads as frosted glass on both iOS and Android.
 * Passing a `null` backdrop falls back to the original opaque surface bar.
 */
@Composable
fun BottomNavigationBar(
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null,
) {
    if (backdrop == null) {
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationBarItems(selectedDestination, destinations, navigateToTopLevelDestination)
        }
        return
    }

    // Track where the bar sits relative to its parent so the captured backdrop can be shifted up by
    // the same amount, lining the blurred slice up exactly with the content drawn behind the bar.
    // Starts as NaN so the backdrop is only drawn once a real position has been measured, avoiding a
    // mis-aligned (top-of-content) blur on the very first frame before onGloballyPositioned fires.
    var barTop by remember { mutableFloatStateOf(Float.NaN) }
    Box(
        modifier = modifier.onGloballyPositioned { barTop = it.positionInParent().y }
    ) {
        // Blurred copy of the content behind the bar. The blur is applied by this node's own
        // graphics layer, so only the backdrop is frosted while the navigation items stay crisp.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    renderEffect = BlurEffect(
                        radiusX = GlassBlurRadius.toPx(),
                        radiusY = GlassBlurRadius.toPx(),
                        edgeTreatment = TileMode.Decal
                    )
                    // Clip to the bar's bounds so the blurred backdrop can't bleed outside the bar.
                    clip = true
                }
                .drawBehind {
                    if (!barTop.isNaN()) {
                        translate(top = -barTop) {
                            drawLayer(backdrop)
                        }
                    }
                }
        )
        // Thin surface tint keeps icons/labels legible over arbitrary content, like Apple's glass.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.surface.copy(alpha = GlassTintAlpha))
        )
        NavigationBar(
            containerColor = Color.Transparent,
            windowInsets = NavigationBarDefaults.windowInsets,
        ) {
            NavigationBarItems(selectedDestination, destinations, navigateToTopLevelDestination)
        }
    }
}

@Composable
private fun RowScope.NavigationBarItems(
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
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
                    painter = destination.selectedIcon,
                    contentDescription = destination.contentDescription,
                    tint = if (!destination.enabled) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                )
            }
        )
    }
}

/**
 * Multiplatform navigation rail for the home screen (used on larger/expanded widths).
 */
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
        Layout(
            modifier = Modifier.widthIn(max = 80.dp),
            content = {
                Column(
                    modifier = Modifier.layoutId(LayoutType.HEADER),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Spacer(Modifier.height(8.dp))
                    Spacer(Modifier.height(4.dp))
                }

                Column(
                    modifier = Modifier.layoutId(LayoutType.CONTENT),
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
                                    painter = destination.selectedIcon,
                                    contentDescription = destination.contentDescription,
                                    tint = if (!destination.enabled) {
                                        LocalContentColor.current.copy(alpha = 0.2f)
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
                    headerPlaceable.placeRelative(0, 0)

                    val nonContentVerticalSpace = constraints.maxHeight - contentPlaceable.height

                    val contentPlaceableY = when (navigationContentPosition) {
                        TOP -> 0
                        CENTER -> nonContentVerticalSpace / 2
                    }
                        .coerceAtLeast(headerPlaceable.height)

                    contentPlaceable.placeRelative(0, contentPlaceableY)
                }
            }
        )
    }
}
