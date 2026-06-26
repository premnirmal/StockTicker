package com.github.premnirmal.ticker.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

class FetchResultTest {

    @Test
    fun success_exposesDataAndNoError() {
        val result = FetchResult.success("AAPL")

        assertTrue(result.wasSuccessful)
        assertFalse(result.hasError)
        assertEquals("AAPL", result.data)
        assertEquals("AAPL", result.dataSafe)
    }

    @Test
    fun failure_exposesErrorAndNoData() {
        val error = IllegalStateException("boom")
        val result = FetchResult.failure<String>(error)

        assertFalse(result.wasSuccessful)
        assertTrue(result.hasError)
        assertSame(error, result.error)
        assertNull(result.dataSafe)
    }

    @Test
    fun success_withNullableType_dataSafeIsNonNull() {
        val result = FetchResult.success(listOf(1, 2, 3))

        assertTrue(result.wasSuccessful)
        assertEquals(listOf(1, 2, 3), result.dataSafe)
    }
}
