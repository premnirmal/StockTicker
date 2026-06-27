package com.github.premnirmal.ticker.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.DevicePosture
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.ticker.ui.WidthSizeClass
import com.github.premnirmal.ticker.ui.calculateContentAndNavigationType
import com.github.premnirmal.ticker.ui.isBookPosture
import com.github.premnirmal.ticker.ui.isSeparating

@Composable
fun calculateContentAndNavigationType(
    widthSizeClass: WindowWidthSizeClass,
    displayFeatures: List<DisplayFeature>
): Pair<NavigationType, ContentType> {
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
    // Delegate the width-only mapping to the shared, fold-free helper, then layer fold handling on
    // top: a half-folded device at the Medium width should show a dual pane.
    val sharedWidthSizeClass = when (widthSizeClass) {
        WindowWidthSizeClass.Compact -> WidthSizeClass.COMPACT
        WindowWidthSizeClass.Medium -> WidthSizeClass.MEDIUM
        WindowWidthSizeClass.Expanded -> WidthSizeClass.EXPANDED
        else -> WidthSizeClass.COMPACT
    }
    val (navigationType, baseContentType) =
        calculateContentAndNavigationType(sharedWidthSizeClass)
    val contentType = if (
        widthSizeClass == WindowWidthSizeClass.Medium &&
        foldingDevicePosture != DevicePosture.NormalPosture
    ) {
        ContentType.DUAL_PANE
    } else {
        baseContentType
    }
    return Pair(navigationType, contentType)
}
