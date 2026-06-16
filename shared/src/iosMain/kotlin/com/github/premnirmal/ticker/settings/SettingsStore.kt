package com.github.premnirmal.ticker.settings

import platform.Foundation.NSUserDefaults

/**
 * Thin key/value store over [NSUserDefaults], the iOS counterpart of Android's `SharedPreferences`.
 *
 * This backs the iOS implementations of the shared Phase 2 settings/persistence interfaces
 * ([com.github.premnirmal.ticker.UserDefaultsPreferences], [com.github.premnirmal.ticker.model.StocksProvider]),
 * mirroring how `AppPreferences`/`StocksProvider` are backed by `SharedPreferences` on Android. A
 * dedicated suite name keeps the keys namespaced to the app, matching the Android `PREFS_NAME`.
 */
class SettingsStore(
    private val defaults: NSUserDefaults = NSUserDefaults(suiteName = SUITE_NAME)
) {

    fun getInt(key: String, default: Int): Int =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key).toInt()

    fun setInt(key: String, value: Int) = defaults.setInteger(value.toLong(), key)

    fun getLong(key: String, default: Long): Long =
        if (defaults.objectForKey(key) == null) default else defaults.integerForKey(key)

    fun setLong(key: String, value: Long) = defaults.setInteger(value, key)

    fun getBoolean(key: String, default: Boolean): Boolean =
        if (defaults.objectForKey(key) == null) default else defaults.boolForKey(key)

    fun setBoolean(key: String, value: Boolean) = defaults.setBool(value, key)

    fun getString(key: String, default: String?): String? =
        defaults.stringForKey(key) ?: default

    fun setString(key: String, value: String?) {
        if (value == null) defaults.removeObjectForKey(key) else defaults.setObject(value, key)
    }

    companion object {
        const val SUITE_NAME = "com.github.premnirmal.ticker"
    }
}
