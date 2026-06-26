package com.github.premnirmal.ticker.navigation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Measurable
import androidx.compose.ui.layout.layoutId
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.offset
import androidx.compose.ui.unit.sp
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

/** Gap between the floating navigation bar and the separate floating search button. */
private val GlassGroupSpacing = 12.dp

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
 *
 * When [searchDestination] is supplied (glass mode only), it is rendered as a separate circular
 * "liquid glass" button on the bottom-right — without a text label — sitting beside the main bar
 * rather than inside it. The bar (carrying the remaining [destinations]) stays on the left.
 */
@Composable
fun BottomNavigationBar(
    selectedDestination: String,
    destinations: List<HomeBottomNavDestination>,
    navigateToTopLevelDestination: (HomeBottomNavDestination) -> Unit,
    modifier: Modifier = Modifier,
    backdrop: GraphicsLayer? = null,
    onHeightChanged: (Dp) -> Unit = {},
    searchDestination: HomeBottomNavDestination? = null,
) {
    if (backdrop == null) {
        // No glass backdrop available: fall back to the opaque Material bar and keep the search
        // destination inline with the rest so it remains reachable.
        val allDestinations = if (searchDestination != null) {
            destinations + searchDestination
        } else {
            destinations
        }
        NavigationBar(
            modifier = modifier,
            containerColor = MaterialTheme.colorScheme.surface,
        ) {
            NavigationBarItems(selectedDestination, allDestinations, navigateToTopLevelDestination)
        }
        return
    }

    val density = LocalDensity.current
    // Lay the floating bar (left, weighted) and the optional separate search button (right) in a
    // single bottom row so they share the same vertical inset/margin and report a combined height.
    Row(
        modifier = modifier
            // Report the full height occupied by the floating row (including the system navigation
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
            ),
        horizontalArrangement = Arrangement.spacedBy(GlassGroupSpacing),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        GlassSurface(
            backdrop = backdrop,
            shape = GlassShape,
            modifier = Modifier.weight(1f),
        ) {
            // Custom compact item row (instead of Material3 NavigationBar, which pins an 80.dp
            // height and a tinted "pill" indicator). This keeps the bar low-profile and lets the
            // selected item read as a frosted-glass highlight rather than a solid container colour.
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
        if (searchDestination != null) {
            // Separate circular glass button on the bottom-right. fillMaxHeight + aspectRatio(1f)
            // makes it a perfect circle matching the bar's height, with no text label.
            GlassSurface(
                backdrop = backdrop,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxHeight()
                    .aspectRatio(1f),
            ) {
                GlassIconButton(
                    destination = searchDestination,
                    selected = selectedDestination == searchDestination.route.route,
                    onClick = { navigateToTopLevelDestination(searchDestination) },
                )
            }
        }
    }
}

/**
 * Reusable "liquid glass" surface: clips its [content] to [shape], frames it with a thin border, and
 * draws a blurred slice of [backdrop] (aligned to this surface's position on screen) beneath a
 * translucent surface tint so it reads as frosted glass. Shared by the floating navigation bar and
 * the separate search button so both blur the same captured home content consistently.
 */
@Composable
private fun GlassSurface(
    backdrop: GraphicsLayer,
    shape: Shape,
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
    // Track where this surface sits relative to the screen so the captured backdrop can be shifted
    // by the same amount, lining the blurred slice up exactly with the content drawn behind it.
    // Starts as Unspecified so the backdrop is only drawn once a real position has been measured,
    // avoiding a mis-aligned blur on the very first frame before onGloballyPositioned fires.
    var offset by remember { mutableStateOf(Offset.Unspecified) }
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    Box(
        modifier = modifier
            .clip(shape)
            .border(1.dp, borderColor, shape)
            .onGloballyPositioned { offset = it.positionInRoot() }
    ) {
        // Blurred copy of the content behind the surface. The blur is applied by this node's own
        // graphics layer, so only the backdrop is frosted while the foreground content stays crisp.
        Spacer(
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    renderEffect = BlurEffect(
                        radiusX = GlassBlurRadius.toPx(),
                        radiusY = GlassBlurRadius.toPx(),
                        edgeTreatment = TileMode.Decal
                    )
                    // Clip to the surface's bounds so the blurred backdrop can't bleed outside it.
                    clip = true
                }
                .drawBehind {
                    if (offset.isSpecified) {
                        translate(left = -offset.x, top = -offset.y) {
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
        content()
    }
}

/**
 * A single circular icon-only button for the separate floating glass action (e.g. Search). Unlike
 * [GlassNavigationItem] it shows no text label. The selected state is marked with the same
 * translucent frosted-glass highlight language as the bar.
 */
@Composable
private fun GlassIconButton(
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
    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(CircleShape)
            .clickable(enabled = destination.enabled, onClick = onClick)
            .then(
                if (selected) {
                    Modifier
                        .background(highlightColor, CircleShape)
                        .border(1.dp, highlightBorder, CircleShape)
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = destination.selectedIcon,
            contentDescription = destination.contentDescription,
            tint = tint,
        )
    }
}

/**
 * A single item in the floating "liquid glass" bottom navigation bar. The selected item is marked by
 * a translucent, softly-bordered rounded highlight (the same frosted-glass language as the bar
 * itself) rather than a solid pink container. Each item shows its label beneath the icon; the label
 * auto-sizes down so longer strings such as "Watchlist" or "Settings" always fit on a single line
 * without truncation or ellipsis.
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
            .padding(horizontal = 6.dp, vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Icon(
            painter = destination.selectedIcon,
            contentDescription = destination.contentDescription,
            tint = tint,
        )
        // Auto-size the label so the whole word stays on one line for any item width. softWrap is
        // disabled and maxLines is 1, so the text shrinks (down to minFontSize) to fit instead of
        // wrapping or showing an ellipsis.
        BasicText(
            text = destination.label,
            modifier = Modifier.fillMaxWidth(),
            color = { tint },
            maxLines = 1,
            softWrap = false,
            autoSize = TextAutoSize.StepBased(
                minFontSize = 7.sp,
                maxFontSize = 11.sp,
                stepSize = 0.5.sp,
            ),
            style = MaterialTheme.typography.labelSmall.copy(textAlign = TextAlign.Center),
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
            },
            label = {
                Text(
                    text = destination.label,
                    style = MaterialTheme.typography.labelSmall,
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
