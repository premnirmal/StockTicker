package com.github.premnirmal.ticker.settings

/**
 * Platform-neutral, synchronous key/value store — the multiplatform abstraction that backs the
 * shared settings/persistence layer ([com.github.premnirmal.ticker.UserPreferences],
 * [com.github.premnirmal.ticker.repo.TickersStore], the iOS `StocksProvider`).
 *
 * It is the common contract that both the legacy native stores (iOS `NSUserDefaults` via
 * `SettingsStore`, Android `SharedPreferences`) and the unified **DataStore Multiplatform**
 * implementation ([DataStorePreferenceStore]) conform to, so the concrete key/value engine can be
 * swapped without touching the consumers. The accessors are intentionally synchronous to match the
 * existing `SharedPreferences`/`NSUserDefaults` semantics the settings code is written against.
 */
interface PreferenceStore {

  fun getInt(key: String, default: Int): Int

  fun setInt(key: String, value: Int)

  fun getLong(key: String, default: Long): Long

  fun setLong(key: String, value: Long)

  fun getBoolean(key: String, default: Boolean): Boolean

  fun setBoolean(key: String, value: Boolean)

  fun getString(key: String, default: String?): String?

  fun setString(key: String, value: String?)
}
