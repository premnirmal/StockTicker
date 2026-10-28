package com.github.premnirmal.ticker.ui

import androidx.compose.runtime.staticCompositionLocalOf

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

val LocalContentType = staticCompositionLocalOf<ContentType> {
    error("No contentType sender provided")
}
