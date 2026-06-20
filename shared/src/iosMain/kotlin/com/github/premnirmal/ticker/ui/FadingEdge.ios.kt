package com.github.premnirmal.ticker.ui

import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp

internal actual fun Modifier.platformFadingEdges(
    topEdgeMaxHeight: Dp,
    topEdgeHeight: State<Dp>,
    bottomEdgeMaxHeight: Dp,
    bottomEdgeHeight: State<Dp>,
): Modifier = drawFadingEdgesGradient(
    topEdgeMaxHeight = topEdgeMaxHeight,
    topEdgeHeight = topEdgeHeight,
    bottomEdgeMaxHeight = bottomEdgeMaxHeight,
    bottomEdgeHeight = bottomEdgeHeight,
)
