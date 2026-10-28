package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.network.CrumbStore
import com.github.premnirmal.ticker.settings.PreferenceStore
import com.github.premnirmal.ticker.settings.SettingsStore

/**
 * iOS entry point for the shared [UserPreferences] settings contract and [CrumbStore] crumb
 * persistence, backed by [NSUserDefaults] via [SettingsStore].
 *
 * The read/write logic itself is fully shared in [SharedUserPreferences]; this class only supplies
 * the iOS-specific default [PreferenceStore]. It is the iOS counterpart of Android's
 * `AppPreferences` and uses the same preference keys and default values so the two platforms behave
 * identically. The configured update window (start/end time and selected days) is consumed by the
 * iOS [com.github.premnirmal.ticker.model.BackgroundRefreshScheduler].
 */
class UserDefaultsPreferences(
    store: PreferenceStore = SettingsStore()
) : SharedUserPreferences(store)
