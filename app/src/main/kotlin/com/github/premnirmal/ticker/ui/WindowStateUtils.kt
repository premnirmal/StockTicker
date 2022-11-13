package com.github.premnirmal.ticker.ui

import android.graphics.Rect
import androidx.window.layout.FoldingFeature
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

sealed interface DevicePosture {
  object NormalPosture : DevicePosture

  data class BookPosture(
    val hingePosition: Rect
  ) : DevicePosture

  data class Separating(
    val hingePosition: Rect,
    var orientation: FoldingFeature.Orientation
  ) : DevicePosture
}

@OptIn(ExperimentalContracts::class)
fun isBookPosture(foldFeature: FoldingFeature?): Boolean {
  contract { returns(true) implies (foldFeature != null) }
  return foldFeature?.state == FoldingFeature.State.HALF_OPENED &&
      foldFeature.orientation == FoldingFeature.Orientation.VERTICAL
}

@OptIn(ExperimentalContracts::class)
fun isSeparating(foldFeature: FoldingFeature?): Boolean {
  contract { returns(true) implies (foldFeature != null) }
  return foldFeature?.state == FoldingFeature.State.FLAT && foldFeature.isSeparating
}

/**
 * Different type of navigation supported by app depending on device size and state.
 */
enum class NavigationType {
  BOTTOM_NAVIGATION, NAVIGATION_RAIL
}

/**
 * Different position of navigation content inside Navigation Rail, Navigation Drawer depending on device size and state.
 */
enum class NavigationContentPosition {
  TOP, CENTER
}

/**
 * App Content shown depending on device size and state.
 */
enum class ContentType {
  SINGLE_PANE, DUAL_PANE
}