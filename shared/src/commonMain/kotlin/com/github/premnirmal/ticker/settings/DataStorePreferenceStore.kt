package com.github.premnirmal.ticker.settings

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.first
import okio.Path.Companion.toPath
import kotlin.concurrent.Volatile

/**
 * Builds a multiplatform DataStore Preferences instance persisted at [producePath] (a fully
 * qualified file path ending in `*.preferences_pb`). The platform Koin modules supply the path
 * (`context.filesDir` on Android, `NSDocumentDirectory` on iOS).
 */
fun createPreferenceDataStore(producePath: () -> String): DataStore<Preferences> =
    PreferenceDataStoreFactory.createWithPath(produceFile = { producePath().toPath() })

/**
 * [PreferenceStore] backed by **DataStore Multiplatform** — the unified replacement for the
 * per-platform key/value stores (iOS `NSUserDefaults`, Android `SharedPreferences`).
 *
 * DataStore is asynchronous (suspend `edit` / `Flow` reads), while the settings code is written
 * against a synchronous store. This bridges the two: the latest [Preferences] snapshot is cached in
 * memory (hydrated once at construction and refreshed after every write, since this store is the
 * sole writer of its file), so reads are synchronous and lock-free while writes block briefly to
 * persist — mirroring `SharedPreferences`/`NSUserDefaults` semantics.
 */
class DataStorePreferenceStore(
    private val dataStore: DataStore<Preferences>
) : PreferenceStore {

    @Volatile
    private var snapshot: Preferences = runBlockingPreferences { dataStore.data.first() }

    override fun getInt(key: String, default: Int): Int =
        snapshot[intPreferencesKey(key)] ?: default

    override fun setInt(key: String, value: Int) = persist {
        it[intPreferencesKey(key)] = value
    }

    override fun getLong(key: String, default: Long): Long =
        snapshot[longPreferencesKey(key)] ?: default

    override fun setLong(key: String, value: Long) = persist {
        it[longPreferencesKey(key)] = value
    }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        snapshot[booleanPreferencesKey(key)] ?: default

    override fun setBoolean(key: String, value: Boolean) = persist {
        it[booleanPreferencesKey(key)] = value
    }

    override fun getString(key: String, default: String?): String? =
        snapshot[stringPreferencesKey(key)] ?: default

    override fun setString(key: String, value: String?) = persist {
        if (value == null) it.remove(stringPreferencesKey(key)) else it[stringPreferencesKey(key)] = value
    }

    private inline fun persist(crossinline mutate: (androidx.datastore.preferences.core.MutablePreferences) -> Unit) {
        snapshot = runBlockingPreferences { dataStore.edit { mutate(it) } }
    }
}

/**
 * Runs [block] to completion on the calling thread. Bridges the synchronous [PreferenceStore]
 * facade onto DataStore's suspend API; `kotlinx.coroutines.runBlocking` is only available on the
 * concurrent (JVM/Native) targets, so it is provided per platform.
 */
internal expect fun <T> runBlockingPreferences(block: suspend () -> T): T
