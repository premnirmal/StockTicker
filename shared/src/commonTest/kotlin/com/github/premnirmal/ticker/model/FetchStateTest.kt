package com.github.premnirmal.ticker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class FetchStateTest {

    @Test
    fun notFetched_displaysPlaceholder() {
        assertEquals("--", FetchState.NotFetched.displayString)
    }

    @Test
    fun failure_displaysExceptionMessage() {
        val state = FetchState.Failure(IllegalStateException("network down"))

        assertEquals("network down", state.displayString)
    }

    @Test
    fun failure_withNullMessage_displaysEmptyString() {
        val state = FetchState.Failure(RuntimeException())

        assertEquals("", state.displayString)
    }

    @Test
    fun success_exposesFetchTimeAndNonEmptyDisplayString() {
        val state = FetchState.Success(fetchTime = 1_700_000_000_000L)

        assertEquals(1_700_000_000_000L, state.fetchTime)
        assertTrue(state.displayString.isNotEmpty())
    }
}
