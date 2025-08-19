package com.github.premnirmal.ticker.home

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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

    override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource
    ): Offset {
        val delta = available.y.toInt()
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
