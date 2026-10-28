package com.github.premnirmal.ticker.ui

import androidx.compose.ui.unit.Dp
import platform.UIKit.UIDevice
import platform.UIKit.UIUserInterfaceIdiomPad

/** True when running on an iPad (regardless of the current window width / multitasking split). */
fun isIpadDevice(): Boolean =
    UIDevice.currentDevice.userInterfaceIdiom == UIUserInterfaceIdiomPad

/**
 * iOS-specific ([NavigationType], [ContentType]) resolution.
 *
 * iPad uses the shared width-based mapping ([calculateContentAndNavigationType]), so Split View /
 * Slide Over still collapse to a single pane when the window is narrow. iPhone always stays a single
 * pane with bottom navigation — even in landscape, where the width can otherwise cross the expanded
 * breakpoint and incorrectly trigger the iPad-style two-pane / navigation-rail layout.
 */
fun iosContentAndNavigationType(width: Dp): Pair<NavigationType, ContentType> =
    if (isIpadDevice()) {
        calculateContentAndNavigationType(widthSizeClassFor(width))
    } else {
        NavigationType.BOTTOM_NAVIGATION to ContentType.SINGLE_PANE
    }

/**
 * [ContentType] for the *quote-detail* screen, which — unlike the home navigation/watchlist — may
 * use its internal two-pane layout (chart on one side, details/positions/news on the other) on an
 * iPhone in landscape.
 *
 * On iPad this defers to the shared width-based mapping (so Split View / Slide Over still collapse
 * to a single pane when narrow). On iPhone it returns [ContentType.DUAL_PANE] once the measured
 * width crosses the Medium breakpoint (600dp) — which every iPhone exceeds in landscape but not in
 * portrait — so the detail screen splits in landscape and stays single-column in portrait.
 */
fun iosQuoteDetailContentType(width: Dp): ContentType =
    if (isIpadDevice()) {
        calculateContentAndNavigationType(widthSizeClassFor(width)).second
    } else if (widthSizeClassFor(width) == WidthSizeClass.COMPACT) {
        ContentType.SINGLE_PANE
    } else {
        ContentType.DUAL_PANE
    }
