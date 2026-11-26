package com.github.premnirmal.ticker.home

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import kotlin.test.Test
import kotlin.test.assertEquals

class CollapsingTopBarScrollConnectionTest {
    @Test
    fun measuredHeaderGrowthIncreasesCollapseRangeWithoutResettingScroll() {
        val connection = CollapsingTopBarScrollConnection(appBarMaxHeight = 160)
        connection.onPreScroll(Offset(0f, -80f), NestedScrollSource.UserInput)

        connection.updateMaxHeight(300)

        assertEquals(-80, connection.appBarOffset)
        val consumed = connection.onPreScroll(Offset(0f, -300f), NestedScrollSource.UserInput)
        assertEquals(-220f, consumed.y)
        assertEquals(-300, connection.appBarOffset)
    }

    @Test
    fun measuredHeaderShrinkClampsOffsetAndCanExpandAgain() {
        val connection = CollapsingTopBarScrollConnection(appBarMaxHeight = 300, initialOffset = -250)

        connection.updateMaxHeight(160)

        assertEquals(-160, connection.appBarOffset)
        val consumed = connection.onPreScroll(Offset(0f, 200f), NestedScrollSource.UserInput)
        assertEquals(160f, consumed.y)
        assertEquals(0, connection.appBarOffset)
    }
}
