package com.github.premnirmal.ticker.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource

class CollapsingTopBarScrollConnection(
    val appBarMaxHeight: Int,
    initialOffset: Int = 0
) : NestedScrollConnection {

    var appBarOffset by mutableIntStateOf(initialOffset)
        private set

    /**
     * Whether the header is allowed to collapse. Set to `false` when the scrolling content fits
     * within the viewport (nothing to scroll), so the header stays fully expanded instead of
     * collapsing on a drag that the inner list can't consume. Expanding an already-collapsed header
     * back open is always allowed.
     */
    var canCollapse by mutableStateOf(true)

    fun resetOffset() {
        appBarOffset = 0
    }

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y.toInt()
        // Don't collapse the header when the content fits on screen; still allow an already
        // collapsed header to expand back (positive delta) so it can't get stuck collapsed.
        if (delta < 0 && !canCollapse) {
            return Offset.Zero
        }
        val newOffset = appBarOffset + delta
        val previousOffset = appBarOffset
        appBarOffset = newOffset.coerceIn(-appBarMaxHeight, 0)
        val consumed = appBarOffset - previousOffset
        return Offset(0f, consumed.toFloat())
    }

    companion object {
        fun saver(maxAppBarHeight: Int) = Saver<CollapsingTopBarScrollConnection, Int>(
            save = { it.appBarOffset },
            restore = { offset ->
                CollapsingTopBarScrollConnection(
                    appBarMaxHeight = maxAppBarHeight,
                    initialOffset = offset
                )
            }
        )
    }
}
