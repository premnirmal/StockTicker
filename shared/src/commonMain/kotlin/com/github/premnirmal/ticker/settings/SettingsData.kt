package com.github.premnirmal.ticker.settings

import com.github.premnirmal.ticker.Time

/**
 * Snapshot of the user's settings, used as immutable state by the shared [SettingsScreen].
 * Parceling was removed because this is only held in-memory by the ViewModel's `StateFlow`.
 */
data class SettingsData(
    val hasWidgets: Boolean,
    val themePref: Int,
    val updateIntervalPref: Int,
    val updateDays: Set<Int>,
    val notificationAlerts: Boolean,
    val startTime: Time,
    val endTime: Time,
    val autoSort: Boolean?,
    val roundToTwoDp: Boolean,
)
