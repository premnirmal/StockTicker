package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.settings.PreferenceStore

/**
 * Simple in-memory [PreferenceStore] for unit tests, replacing the previous direct use of
 * `SharedPreferences` when constructing [AppPreferences].
 */
class FakePreferenceStore : PreferenceStore {

    private val values = mutableMapOf<String, Any?>()

    override fun getInt(key: String, default: Int): Int = values[key] as? Int ?: default

    override fun setInt(key: String, value: Int) { values[key] = value }

    override fun getLong(key: String, default: Long): Long = values[key] as? Long ?: default

    override fun setLong(key: String, value: Long) { values[key] = value }

    override fun getBoolean(key: String, default: Boolean): Boolean =
        values[key] as? Boolean ?: default

    override fun setBoolean(key: String, value: Boolean) { values[key] = value }

    override fun getString(key: String, default: String?): String? =
        values[key] as? String ?: default

    override fun setString(key: String, value: String?) {
        if (value == null) values.remove(key) else values[key] = value
    }
}
