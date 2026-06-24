package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.BlurEffect
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import com.github.premnirmal.ticker.ui.NavigationContentPosition
import com.github.premnirmal.ticker.ui.NavigationContentPosition.CENTER
import com.github.premnirmal.ticker.ui.NavigationContentPosition.TOP

/** Blur radius applied to the content captured behind the glass bottom navigation bar. */
private val GlassBlurRadius = 24.dp

/** Opacity of the surface tint layered over the blurred backdrop to keep items legible. */
private const val GlassTintAlpha = 0.55f

/** Corner radius giving the floating bar its rounded "pill" silhouette. */
private val GlassCornerRadius = 28.dp

/** Horizontal inset so the floating bar does not span the full screen width. */
private val GlassHorizontalMargin = 16.dp

/** Gap between the floating bar and the bottom edge (added on top of the system nav inset). */
private val GlassBottomMargin = 8.dp

/** Rounded "pill" silhouette of the floating bar; hoisted so it is allocated once. */
private val GlassShape = RoundedCornerShape(GlassCornerRadius)

/** Inner padding around the row of navigation items, keeping the bar compact. */
private val GlassContentPadding = 6.dp

/** Corner radius of the translucent highlight drawn behind the selected item. */
private val GlassSelectedCornerRadius = 22.dp

/** Rounded silhouette of the selected-item highlight; hoisted so it is allocated once. */
private val GlassSelectedShape = RoundedCornerShape(GlassSelectedCornerRadius)

/**
 * Vertical space occupied by the floating "liquid glass" bottom navigation bar (its measured height
 * including the system navigation inset and bottom margin). Home content is rendered edge-to-edge
 * beneath the floating bar, so scrollable screens add this as bottom content padding to ensure their
 * last item can scroll clear of the bar instead of staying hidden behind it. Defaults to `0.dp` when
 * no floating bar is present (e.g. the navigation-rail layout), so screens can read it unconditionally.
 */
val LocalContentBottomPadding = compositionLocalOf { 0.dp }

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
 * bar renders as a floating, rounded "liquid glass" pill: it is inset from the screen edges (so it
 * does not span the full width), clipped to rounded corners, and floats above the content + system
 * navigation inset. The slice of content sitting behind the pill is drawn blurred underneath a thin
 * surface tint, so the bar reads as frosted glass on both iOS and Android. Passing a `null` backdrop
 * falls back to the original opaque, full-width surface bar.
 */
@Composable
fun BottomNavigationBar(
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null,
    onHeightChanged: (Dp) -> Unit = {},
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

    // Track where the pill sits relative to its parent so the captured backdrop can be shifted by the
    // same amount, lining the blurred slice up exactly with the content drawn behind the pill (both
    // horizontally and vertically, since the pill is inset from the screen edges). Starts as
    // Unspecified so the backdrop is only drawn once a real position has been measured, avoiding a
    // mis-aligned blur on the very first frame before onGloballyPositioned fires.
    var barOffset by remember { mutableStateOf(Offset.Unspecified) }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val density = LocalDensity.current
    Box(
        modifier = modifier
            // Report the full height occupied by the floating bar (including the system navigation
            // inset and bottom margin added below) so home content can pad its scrollable lists by
            // the same amount. Measured before those paddings are applied so the reported value
            // covers the whole region the bar overlays.
            .onSizeChanged { size -> onHeightChanged(with(density) { size.height.toDp() }) }
            // Float above the system navigation inset, then add margins so the pill clears the
            // screen edges and does not span the full width.
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(
                start = GlassHorizontalMargin,
                end = GlassHorizontalMargin,
                bottom = GlassBottomMargin,
            )
            .clip(GlassShape)
            .border(1.dp, borderColor, GlassShape)
            .onGloballyPositioned { barOffset = it.positionInParent() }
    ) {
        // Blurred copy of the content behind the pill. The blur is applied by this node's own
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
                    // Clip to the pill's bounds so the blurred backdrop can't bleed outside the pill.
                    clip = true
                }
                .drawBehind {
                    if (barOffset.isSpecified) {
                        translate(left = -barOffset.x, top = -barOffset.y) {
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
        // Custom compact item row (instead of Material3 NavigationBar, which pins an 80.dp height and
        // a tinted "pill" indicator). This keeps the bar low-profile and lets the selected item read
        // as a frosted-glass highlight rather than a solid container colour.
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(GlassContentPadding),
            horizontalArrangement = Arrangement.spacedBy(GlassContentPadding),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            destinations.forEach { destination ->
                GlassNavigationItem(
                    destination = destination,
                    selected = selectedDestination == destination.route.route,
                    onClick = { navigateToTopLevelDestination(destination) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

/**
 * A single item in the floating "liquid glass" bottom navigation bar. The selected item is marked by
 * a translucent, softly-bordered rounded highlight (the same frosted-glass language as the bar
 * itself) rather than a solid pink container. No label is shown in any state.
 */
@Composable
private fun GlassNavigationItem(
    destination: HomeBottomNavDestination,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tint = when {
        !destination.enabled -> MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        else -> MaterialTheme.colorScheme.primary
    }
    val highlightColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.10f)
    val highlightBorder = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.18f)
    Column(
        modifier = modifier
            .clip(GlassSelectedShape)
            .clickable(enabled = destination.enabled, onClick = onClick)
            .then(
                if (selected) {
                    Modifier
                        .background(highlightColor, GlassSelectedShape)
                        .border(1.dp, highlightBorder, GlassSelectedShape)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = destination.selectedIcon,
            contentDescription = destination.contentDescription,
            tint = tint,
        )
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
