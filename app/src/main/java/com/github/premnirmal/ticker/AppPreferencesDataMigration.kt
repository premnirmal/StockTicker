package com.github.premnirmal.ticker

import android.content.SharedPreferences
import androidx.datastore.core.DataMigration
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey

/**
 * One-shot DataStore migration that imports the settings [AppPreferences] used to read from the
 * legacy [SharedPreferences] file ([AppPreferences.PREFS_NAME]) into the unified DataStore
 * Preferences store. It only copies the keys that [AppPreferences] owns, leaving the keys still read
 * directly from `SharedPreferences` by other components (e.g. `StocksProvider`, the tickers store)
 * untouched.
 *
 * The `UPDATE_DAYS` key was persisted as a `Set<String>` in `SharedPreferences`; DataStore's
 * synchronous [com.github.premnirmal.ticker.settings.PreferenceStore] facade stores it as a
 * comma-separated string, so this migration performs that conversion.
 */
class AppPreferencesDataMigration(
    private val sharedPreferences: SharedPreferences
) : DataMigration<Preferences> {

    private val migratedKey = booleanPreferencesKey(MIGRATED_KEY)

    override suspend fun shouldMigrate(currentData: Preferences): Boolean =
        currentData[migratedKey] != true

    override suspend fun migrate(currentData: Preferences): Preferences {
        val mutable = currentData.toMutablePreferences()
        val all = sharedPreferences.all
        for (key in INT_KEYS) {
            if (mutable.contains(intPreferencesKey(key))) continue
            (all[key] as? Int)?.let { mutable[intPreferencesKey(key)] = it }
        }
        for (key in BOOLEAN_KEYS) {
            if (mutable.contains(booleanPreferencesKey(key))) continue
            (all[key] as? Boolean)?.let { mutable[booleanPreferencesKey(key)] = it }
        }
        for (key in STRING_KEYS) {
            if (mutable.contains(stringPreferencesKey(key))) continue
            (all[key] as? String)?.let { mutable[stringPreferencesKey(key)] = it }
        }
        if (!mutable.contains(stringPreferencesKey(AppPreferences.UPDATE_DAYS))) {
            @Suppress("UNCHECKED_CAST")
            val days = all[AppPreferences.UPDATE_DAYS] as? Set<String>
            if (!days.isNullOrEmpty()) {
                mutable[stringPreferencesKey(AppPreferences.UPDATE_DAYS)] =
                    days.mapNotNull { it.toIntOrNull() }.sorted().joinToString(",")
            }
        }
        mutable[migratedKey] = true
        return mutable
    }

    override suspend fun cleanUp() {
        // Keep the legacy values in place; only AppPreferences read these keys and it now reads from
        // DataStore. Removing them is unnecessary and avoids any risk to the shared prefs file.
    }

    @Suppress("UNCHECKED_CAST")
    private fun Preferences.toMutablePreferences() =
        mutablePreferencesOf(
            *asMap().map { (k, v) -> (k as Preferences.Key<Any>) to v }.toTypedArray()
        )

    companion object {
        const val MIGRATED_KEY = "shared_prefs_migrated"

        private val INT_KEYS = listOf(
            AppPreferences.APP_VERSION_CODE,
            AppPreferences.UPDATE_INTERVAL,
            AppPreferences.APP_THEME,
            AppPreferences.PREFERENCE_SHOWN_ADD_REMOVE_TOOLTIP
        )

        private val BOOLEAN_KEYS = listOf(
            AppPreferences.WIDGET_REFRESHING,
            AppPreferences.TUTORIAL_SHOWN,
            AppPreferences.SETTING_ROUND_TWO_DP,
            AppPreferences.SETTING_NOTIFICATION_ALERTS
        )

        private val STRING_KEYS = listOf(
            AppPreferences.START_TIME,
            AppPreferences.END_TIME,
            AppPreferences.CRUMB
        )
    }
}
