package com.github.premnirmal.ticker.settings

import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.intPreferencesKey
import okio.FileSystem
import okio.Path
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataStorePreferenceStoreTest {

    private fun newPath(): Path {
        // Use an absolute path under the system temporary directory. A relative path is not
        // writable from the iOS simulator's working directory and makes DataStore throw.
        val dir = FileSystem.SYSTEM_TEMPORARY_DIRECTORY / "stockticker-test-datastore"
        FileSystem.SYSTEM.createDirectories(dir)
        return dir / "prefs-${kotlin.random.Random.nextInt()}.preferences_pb"
    }

    private fun newStore(): PreferenceStore =
        DataStorePreferenceStore(createPreferenceDataStore { newPath().toString() })

    @Test
    fun corruptPreferencesAreReplacedAndRemainWritable() {
        val path = newPath()
        FileSystem.SYSTEM.write(path) { writeByte(0) }
        val store = DataStorePreferenceStore(createPreferenceDataStore { path.toString() })

        assertEquals(7, store.getInt("missing", 7))
        store.setString("s", "recovered")
        assertEquals("recovered", store.getString("s", null))
    }

    @Test
    fun migrationsRunAfterCorruptionRecovery() {
        val path = newPath()
        FileSystem.SYSTEM.write(path) { writeByte(0) }
        val key = intPreferencesKey("legacy")
        val migration = object : DataMigration<Preferences> {
            override suspend fun shouldMigrate(currentData: Preferences) = !currentData.contains(key)
            override suspend fun migrate(currentData: Preferences): Preferences =
                currentData.toMutablePreferences().apply { this[key] = 42 }
            override suspend fun cleanUp() = Unit
        }
        val store = DataStorePreferenceStore(
            createPreferenceDataStore(migrations = listOf(migration)) { path.toString() }
        )

        assertEquals(42, store.getInt("legacy", 0))
    }

    @Test
    fun returnsDefaultsWhenUnset() {
        val store = newStore()
        assertEquals(7, store.getInt("missing", 7))
        assertEquals(42L, store.getLong("missing", 42L))
        assertTrue(store.getBoolean("missing", true))
        assertNull(store.getString("missing", null))
        assertEquals("fallback", store.getString("missing", "fallback"))
    }

    @Test
    fun roundTripsEachType() {
        val store = newStore()
        store.setInt("i", 3)
        store.setLong("l", 9_000_000_000L)
        store.setBoolean("b", false)
        store.setString("s", "hello")
        assertEquals(3, store.getInt("i", 0))
        assertEquals(9_000_000_000L, store.getLong("l", 0L))
        assertEquals(false, store.getBoolean("b", true))
        assertEquals("hello", store.getString("s", null))
    }

    @Test
    fun settingNullStringRemovesIt() {
        val store = newStore()
        store.setString("s", "value")
        assertEquals("value", store.getString("s", null))
        store.setString("s", null)
        assertNull(store.getString("s", null))
    }
}
