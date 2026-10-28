package com.github.premnirmal.ticker.ui

import androidx.compose.ui.unit.dp
import kotlin.test.Test
import kotlin.test.assertEquals

class WindowSizeTest {

    @Test
    fun widthSizeClassUsesMaterialBreakpoints() {
        assertEquals(WidthSizeClass.COMPACT, widthSizeClassFor(0.dp))
        assertEquals(WidthSizeClass.COMPACT, widthSizeClassFor(599.dp))
        assertEquals(WidthSizeClass.MEDIUM, widthSizeClassFor(600.dp))
        assertEquals(WidthSizeClass.MEDIUM, widthSizeClassFor(839.dp))
        assertEquals(WidthSizeClass.EXPANDED, widthSizeClassFor(840.dp))
        assertEquals(WidthSizeClass.EXPANDED, widthSizeClassFor(1280.dp))
    }

    @Test
    fun compactIsSinglePaneWithBottomNavigation() {
        val (navigationType, contentType) =
            calculateContentAndNavigationType(WidthSizeClass.COMPACT)
        assertEquals(NavigationType.BOTTOM_NAVIGATION, navigationType)
        assertEquals(ContentType.SINGLE_PANE, contentType)
    }

    @Test
    fun mediumIsSinglePaneWithBottomNavigation() {
        val (navigationType, contentType) =
            calculateContentAndNavigationType(WidthSizeClass.MEDIUM)
        assertEquals(NavigationType.BOTTOM_NAVIGATION, navigationType)
        assertEquals(ContentType.SINGLE_PANE, contentType)
    }

    @Test
    fun expandedIsDualPaneWithNavigationRail() {
        val (navigationType, contentType) =
            calculateContentAndNavigationType(WidthSizeClass.EXPANDED)
        assertEquals(NavigationType.NAVIGATION_RAIL, navigationType)
        assertEquals(ContentType.DUAL_PANE, contentType)
    }
}
