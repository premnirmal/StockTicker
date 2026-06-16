package com.github.premnirmal.ticker.settings

import platform.Foundation.NSUserDefaults

/**
 * [PreferenceStore] over [NSUserDefaults], the iOS counterpart of Android's `SharedPreferences`.
 *
 * This is the legacy native iOS key/value store; production wiring now defaults to the unified
 * [DataStorePreferenceStore] (DataStore Multiplatform), but this remains a valid [PreferenceStore]
 * implementation (e.g. used directly in tests). A dedicated suite name keeps the keys namespaced to
 * the app, matching the Android `PREFS_NAME`.
 */
class SettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SUITE_NAME)
) : PreferenceStore {

    override fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()

    override fun setInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)

    override fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key)

    override fun setLong(key: String, value: Long) = defaults.setInteger(value, key)

    override fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    override fun setBoolean(key: String, value: Boolean) = defaults.setBool(value, key)

    override fun getString(key: String, default: String?): String? =
        defaults.stringForKey(key) ?: default

    override fun setString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }

    companion object {
        const val SUITE_NAME = "com.github.premnirmal.ticker"
    }
}
