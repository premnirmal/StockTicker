package com.github.premnirmal.ticker.test

import com.github.premnirmal.ticker.Time
import com.github.premnirmal.ticker.UserPreferences
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * In-memory [UserPreferences] used by the shared ViewModel tests. Only the bits the ViewModels touch
 * carry real behaviour (the add/remove tooltip flow and the theme pref); the rest return sensible
 * defaults so the contract is satisfied without a platform key/value store.
 */
class FakeUserPreferences : UserPreferences {

    override val updateIntervalMs: Long = 0L
    override var updateIntervalPref: Int = 0

    private val _isRefreshing = MutableStateFlow(false)
    override val isRefreshing: StateFlow<Boolean> = _isRefreshing
    override fun setRefreshing(refreshing: Boolean) {
        _isRefreshing.value = refreshing
    }

    private var tutorialShown = false
    override fun tutorialShown(): Boolean = tutorialShown
    override fun setTutorialShown(shown: Boolean) {
        tutorialShown = shown
    }

    override fun shouldPromptRate(): Boolean = false

    private var lastSavedVersionCode = -1
    override fun getLastSavedVersionCode(): Int = lastSavedVersionCode
    override fun saveVersionCode(code: Int) {
        lastSavedVersionCode = code
    }

    private var roundToTwoDecimalPlaces = true
    override fun roundToTwoDecimalPlaces(): Boolean = roundToTwoDecimalPlaces
    override fun setRoundToTwoDecimalPlaces(round: Boolean) {
        roundToTwoDecimalPlaces = round
    }

    private var notificationAlerts = true
    override fun notificationAlerts(): Boolean = notificationAlerts
    override fun setNotificationAlerts(set: Boolean) {
        notificationAlerts = set
    }

    private val _autoSort = MutableStateFlow(false)
    override val autoSortFlow: StateFlow<Boolean> = _autoSort
    override fun autoSort(): Boolean = _autoSort.value
    override fun setAutoSort(autoSort: Boolean) {
        _autoSort.value = autoSort
    }

    private val _themePref = MutableStateFlow(UserPreferences.FOLLOW_SYSTEM_THEME)
    override var themePref: Int
        get() = _themePref.value
        set(value) {
            _themePref.value = value
        }
    override val themePrefFlow: Flow<Int> = _themePref

    private val _showAddRemoveTooltip = MutableStateFlow(true)
    override val showAddRemoveTooltip: Flow<Boolean> = _showAddRemoveTooltip
    var addRemoveTooltipShownCount: Int = 0
        private set

    override fun setAddRemoveTooltipShown() {
        addRemoveTooltipShownCount++
        _showAddRemoveTooltip.value = false
    }

    private var startTime = Time(9, 30)
    override fun startTime(): Time = startTime
    override fun setStartTime(time: String) {
        startTime = Time.parse(time)
    }

    private var endTime = Time(16, 0)
    override fun endTime(): Time = endTime
    override fun setEndTime(time: String) {
        endTime = Time.parse(time)
    }

    private var updateDays: Set<Int> = setOf(1, 2, 3, 4, 5)
    override fun updateDays(): Set<Int> = updateDays
    override fun setUpdateDays(days: Set<Int>) {
        updateDays = days
    }
}
