package com.github.premnirmal.ticker.repo

import com.github.premnirmal.ticker.repo.migrations.MIGRATION_1_2
import com.github.premnirmal.ticker.repo.migrations.MIGRATION_8_9
import com.github.premnirmal.ticker.repo.migrations.allMigrations
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Guards the Room KMP migration chain ported from the previous Android-only `SupportSQLiteDatabase`
 * migrations. The chain must stay contiguous from version 1 up to the current [QuotesDB] version so
 * existing installs migrate transparently on every platform.
 */
class MigrationsTest {

    private val databaseVersion = 9

    @Test
    fun allMigrations_formContiguousChainUpToDatabaseVersion() {
        assertEquals(databaseVersion - 1, allMigrations.size)
        allMigrations.forEachIndexed { index, migration ->
            val expectedStart = index + 1
            assertEquals(expectedStart, migration.startVersion, "migration #$index start")
            assertEquals(expectedStart + 1, migration.endVersion, "migration #$index end")
        }
    }

    @Test
    fun allMigrations_areOrderedAndCoverEndpoints() {
        assertEquals(1, allMigrations.first().startVersion)
        assertEquals(databaseVersion, allMigrations.last().endVersion)
        assertTrue(allMigrations.contains(MIGRATION_1_2))
        assertTrue(allMigrations.contains(MIGRATION_8_9))
    }

    @Test
    fun databaseName_isStable() {
        // The on-disk file name must not change or existing Android installs would lose their data.
        assertEquals("quotes-db", QUOTES_DB_NAME)
    }
}
