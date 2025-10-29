package com.mnikonov.fade_out

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.ScrollableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import org.intellij.lang.annotations.Language

/**
 * Credit to https://github.com/nikonovmi/compose-fading-edges
 *
 * This is the plot of y = ((1 - x)^3 + 3(1 - x)^2 * x)^2, where x is in the interval [0, 1].
 * We use this function to create truly beautiful non-linear fade-out gradients.
 * ****
 *     **
 *       **
 *         **
 *           *
 *            **
 *              *
 *               *
 *                **
 *                  *
 *                   *
 *                    **
 *                      *
 *                       **
 *                         *
 *                          **
 *                            ***
 *                               **
 *                                 *****
 *                                      *************
 */
@Language(value = "AGSL")
private val bottomFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float bottomFade;
    half4 main(float2 coord) {
        if (bottomFade < 1) {
            return BLACK_COLOR; // no fading needed at all
        } else if (coord.y < resolution.y - bottomFade) {
            return BLACK_COLOR; // no fading needed outside of the fading edge area
        } else {
            // formula: y = ((1 - x)^3 + 3(1 - x)^2 * x)^2
            float x = ((resolution.y - coord.y) / bottomFade); // x is 0 at the BOTTOM
            float y = (1.0 - x) * (1.0 - x) * (1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float alpha = 1.0 - y * y; 
            return half4(0, 0, 0, alpha); // return black color with the calculated alpha
        }
    }
"""

@Language(value = "AGSL")
private val topFadingEdgeShader = """
    const half4 BLACK_COLOR = half4(0, 0, 0, 1);
    uniform float2 resolution;
    uniform float topFade;
    half4 main(float2 coord) {
        if (topFade < 1) {
            return BLACK_COLOR; // no fading needed at all
        } else if (coord.y > topFade) {
            return BLACK_COLOR; // no fading needed outside of the fading edge area
        } else {
            // formula: y = ((1 - x)^3 + 3(1 - x)^2 * x)^2
            float x = coord.y / topFade; // x is 0 at the TOP
            float y = (1.0 - x)*(1.0 - x)*(1.0 - x) + 3.0 * (1.0 - x) * (1.0 - x) * x;
            float alpha = 1.0 - y * y; 
            return half4(0, 0, 0, alpha); // return black color with the calculated alpha
        }
    }
"""

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
    if (Build.VERSION.SDK_INT >= 33) {
        this.drawFadingEdgesApi33(
            topEdgeMaxHeight = topEdgeHeight,
            topEdgeHeight = animatedTopEdgeHeight,
            bottomEdgeMaxHeight = bottomEdgeHeight,
            bottomEdgeHeight = animatedBottomEdgeHeight,
        )
    } else {
        this.drawFadingEdgesOldApi(
            topEdgeMaxHeight = topEdgeHeight,
            topEdgeHeight = animatedTopEdgeHeight,
            bottomEdgeMaxHeight = bottomEdgeHeight,
            bottomEdgeHeight = animatedBottomEdgeHeight,
        )
    }
}

@Stable
@RequiresApi(33)
private fun Modifier.drawFadingEdgesApi33(
    topEdgeMaxHeight: Dp,
    topEdgeHeight: State<Dp>,
    bottomEdgeMaxHeight: Dp,
    bottomEdgeHeight: State<Dp>,
) = then(
    Modifier
        .graphicsLayer(compositingStrategy = CompositingStrategy.Offscreen)
        .drawWithCache {
            val topEdgeShader = RuntimeShader(topFadingEdgeShader)
            topEdgeShader.setFloatUniform("resolution", size.width, size.height)
            val topEdgeBrush = ShaderBrush(topEdgeShader)

            val bottomEdgeShader = RuntimeShader(bottomFadingEdgeShader)
            bottomEdgeShader.setFloatUniform("resolution", size.width, size.height)
            val bottomEdgeBrush = ShaderBrush(bottomEdgeShader)

            onDrawWithContent {
                drawContent()
                if (topEdgeMaxHeight.toPx() < size.height) {
                    topEdgeShader.setFloatUniform("topFade", topEdgeHeight.value.toPx())
                    drawRect(
                        brush = topEdgeBrush,
                        blendMode = BlendMode.DstIn,
                    )
                }

                if (bottomEdgeMaxHeight.toPx() < size.height) {
                    bottomEdgeShader.setFloatUniform("bottomFade", bottomEdgeHeight.value.toPx())
                    drawRect(
                        brush = bottomEdgeBrush,
                        blendMode = BlendMode.DstIn,
                    )
                }
            }
        }
)

@Stable
private fun Modifier.drawFadingEdgesOldApi(
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