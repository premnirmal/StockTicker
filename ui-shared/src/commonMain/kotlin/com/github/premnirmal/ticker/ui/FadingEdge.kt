package com.github.premnirmal.ticker.ui

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Animated top/bottom fading edges for a scrollable container: the edges fade in/out as the
 * [state] can scroll backward/forward. The animated edge heights and the public modifier are pure
 * multiplatform `animation`/`foundation`/`compose.ui` APIs, so they live in `:shared` `commonMain`
 * and the shared Compose UI (and iOS) can reuse them. The actual edge rendering is delegated to a
 * platform [platformFadingEdges] seam: Android uses an AGSL `RuntimeShader` fast path on API 33+
 * (falling back to the gradient draw otherwise), while iOS uses the gradient draw.
 *
 * Credit to https://github.com/nikonovmi/compose-fading-edges
 */
fun Modifier.fadingEdges(
    state: ScrollableState,
    topEdgeHeight: Dp = 28.dp,
    bottomEdgeHeight: Dp = 28.dp,
) = composed {
    val animatedTopEdgeHeight = animateDpAsState(
        targetValue = if (state.canScrollBackward) topEdgeHeight else 0.dp,
        animationSpec = tween(360),
        label = "TopFadeDpAnimation"
    )
    val animatedBottomEdgeHeight = animateDpAsState(
        targetValue = if (state.canScrollForward) bottomEdgeHeight else 0.dp,
        animationSpec = tween(360),
        label = "BottomFadeDpAnimation"
    )
    this.platformFadingEdges(
        topEdgeMaxHeight = topEdgeHeight,
        topEdgeHeight = animatedTopEdgeHeight,
        bottomEdgeMaxHeight = bottomEdgeHeight,
        bottomEdgeHeight = animatedBottomEdgeHeight,
    )
}

/**
 * Platform seam that renders the fading edges. Android additionally provides an AGSL
 * `RuntimeShader` fast path on API 33+; both Android (pre-33) and iOS reuse [drawFadingEdgesGradient].
 */
internal expect fun Modifier.platformFadingEdges(
    topEdgeMaxHeight: Dp,
    topEdgeHeight: State<Dp>,
    bottomEdgeMaxHeight: Dp,
    bottomEdgeHeight: State<Dp>,
): Modifier

/**
 * Pure multiplatform gradient implementation of the fading edges: draws a vertical
 * transparent↔black gradient masked with [BlendMode.DstIn] at the top and bottom edges.
 */
@Stable
internal fun Modifier.drawFadingEdgesGradient(
    topEdgeMaxHeight: Dp,
    topEdgeHeight: State<Dp>,
    bottomEdgeMaxHeight: Dp,
    bottomEdgeHeight: State<Dp>,
) = then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithContent {
            drawContent()

            val topEdgeHeightPx = topEdgeHeight.value.toPx()
            if (topEdgeMaxHeight.toPx() < size.height && topEdgeHeightPx > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black),
                        startY = 0f,
                        endY = topEdgeHeightPx,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }

            val bottomEdgeHeightPx = bottomEdgeHeight.value.toPx()
            if (bottomEdgeMaxHeight.toPx() < size.height && bottomEdgeHeightPx > 1f) {
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Black, Color.Transparent),
                        startY = size.height - bottomEdgeHeightPx,
                        endY = size.height,
                    ),
                    blendMode = BlendMode.DstIn,
                )
            }
        }
)
