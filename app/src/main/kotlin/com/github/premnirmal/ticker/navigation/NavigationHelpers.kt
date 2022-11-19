package com.github.premnirmal.ticker.navigation

import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.window.layout.DisplayFeature
import androidx.window.layout.FoldingFeature
import com.github.premnirmal.ticker.ui.ContentType
import com.github.premnirmal.ticker.ui.DevicePosture
import com.github.premnirmal.ticker.ui.NavigationType
import com.github.premnirmal.ticker.ui.isBookPosture
import com.github.premnirmal.ticker.ui.isSeparating

enum class LayoutType {
  HEADER,
  CONTENT
}

@Composable
fun CalculateContentAndNavigationType(
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
  val contentType: ContentType
  val navigationType: NavigationType
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
  return Pair(navigationType, contentType)
}
