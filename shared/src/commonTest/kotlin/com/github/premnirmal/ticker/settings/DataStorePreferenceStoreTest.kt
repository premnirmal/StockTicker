package com.github.premnirmal.ticker.settings

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataStorePreferenceStoreTest {

    private fun newStore(): PreferenceStore {
        val path = "build/test-datastore/prefs-${kotlin.random.Random.nextInt()}.preferences_pb"
        return DataStorePreferenceStore(createPreferenceDataStore { path })
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
