package com.github.premnirmal.ticker.ui

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Multiplatform width size class, mirroring `androidx.compose.material3.windowsizeclass`
 * `WindowWidthSizeClass`. It is computed from a measured width (see [widthSizeClassFor]) so it works
 * on platforms — like iOS — that do not expose the Android window-size-class APIs, and so it tracks
 * the app's *current* width (important for iPad Slide Over / Split View multitasking) rather than the
 * physical device idiom.
 */
enum class WidthSizeClass {
    COMPACT, MEDIUM, EXPANDED
}

/**
 * Maps a measured available [width] to a [WidthSizeClass] using the same breakpoints as Material 3's
 * `WindowWidthSizeClass` (600dp / 840dp).
 */
fun widthSizeClassFor(width: Dp): WidthSizeClass = when {
    width < 600.dp -> WidthSizeClass.COMPACT
    width < 840.dp -> WidthSizeClass.MEDIUM
    else -> WidthSizeClass.EXPANDED
}

/**
 * Fold-free width → ([NavigationType], [ContentType]) mapping shared by Android and iOS. Android's
 * `calculateContentAndNavigationType(WindowWidthSizeClass, List<DisplayFeature>)` overload delegates
 * to this and then layers fold-posture handling on top (foldables can show a dual pane at the Medium
 * width). On iOS — which has no folds — this width-only mapping is used directly.
 */
fun calculateContentAndNavigationType(
    widthSizeClass: WidthSizeClass
): Pair<NavigationType, ContentType> = when (widthSizeClass) {
    WidthSizeClass.COMPACT -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    WidthSizeClass.MEDIUM -> NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    WidthSizeClass.EXPANDED -> NavigationType.NAVIGATION_RAIL to ContentType.DUAL_PANE
}
