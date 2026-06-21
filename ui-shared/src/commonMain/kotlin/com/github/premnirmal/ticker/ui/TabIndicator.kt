package com.github.premnirmal.ticker.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.TabPosition
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.platform.debugInspectorInfo

/**
 * Animated indicator offset for the watchlist's widget tab row: a one-third-width underline that
 * slides/resizes towards the selected [currentTabPosition]. It is built entirely from multiplatform
 * `animation`/`foundation`/`material3` APIs (no Android coupling), so it lives in `:shared`
 * `commonMain` and the shared watchlist UI (and iOS) can reuse it.
 */
fun Modifier.customTabIndicatorOffset(
    currentTabPosition: TabPosition
): Modifier = composed(
    inspectorInfo = debugInspectorInfo {
        name = "customTabIndicatorOffset"
        value = currentTabPosition
    }
) {
    val currentTabWidth by animateDpAsState(
        targetValue = currentTabPosition.width * 0.33f,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
        label = ""
    )
    val indicatorOffset by animateDpAsState(
        targetValue = currentTabPosition.left,
        animationSpec = tween(durationMillis = 150, easing = FastOutLinearInEasing),
        label = ""
    )
    wrapContentSize(Alignment.BottomStart)
        .offset(x = indicatorOffset + currentTabPosition.width * 0.33f)
        .width(currentTabWidth)
}
