package com.github.premnirmal.ticker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame

class FetchExceptionTest {

    @Test
    fun carriesMessageAndCause() {
        val cause = IllegalArgumentException("bad input")
        val exception = FetchException("fetch failed", cause)

        assertEquals("fetch failed", exception.message)
        assertSame(cause, exception.cause)
    }

    @Test
    fun isCaughtAsAGenericException() {
        val caught: Exception? = try {
            throw FetchException("fetch failed")
        } catch (ex: Exception) {
            ex
        }

        assertEquals("fetch failed", caught?.message)
    }
}
