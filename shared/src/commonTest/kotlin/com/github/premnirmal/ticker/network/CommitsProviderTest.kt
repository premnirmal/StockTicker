package com.github.premnirmal.ticker.network

import com.github.premnirmal.ticker.model.FetchResult
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class CommitsProviderTest {

    @Test
    fun loadWhatsNew_splitsLinesAndFiltersBotCommits() {
        val changeLog = listOf(
            "Add dark theme",
            "Updated version.properties for 4.0.0",
            "Fix crash on launch",
            "F-droid build metadata",
            "Improve chart rendering"
        ).joinToString("\n")

        val result = CommitsProvider(changeLog).loadWhatsNew()

        assertTrue(result.wasSuccessful)
        assertEquals(
            listOf("Add dark theme", "Fix crash on launch", "Improve chart rendering"),
            result.data
        )
    }

    @Test
    fun loadWhatsNew_emptyChangeLogReturnsSingleBlankLine() {
        val result = CommitsProvider("").loadWhatsNew()

        assertTrue(result.wasSuccessful)
        assertEquals(listOf(""), result.data)
    }
}
