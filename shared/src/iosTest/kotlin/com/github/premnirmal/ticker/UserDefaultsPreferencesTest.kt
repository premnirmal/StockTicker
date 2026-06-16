package com.github.premnirmal.ticker

import com.github.premnirmal.ticker.settings.SettingsStore
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UserDefaultsPreferencesTest {

    private val suiteName = "test-prefs-${kotlin.random.Random.nextInt()}"
    private val defaults = NSUserDefaults(suiteName = suiteName)
    private val preferences = UserDefaultsPreferences(SettingsStore(defaults))

    @AfterTest
    fun tearDown() {
        defaults.removePersistentDomainForName(suiteName)
    }

    @Test
    fun defaultsMatchAndroid() {
        assertEquals(15 * 60 * 1000L, preferences.updateIntervalMs)
        assertEquals(1, preferences.updateIntervalPref)
        assertTrue(preferences.roundToTwoDecimalPlaces())
        assertTrue(preferences.notificationAlerts())
        assertFalse(preferences.tutorialShown())
        assertEquals(UserDefaultsPreferences.FOLLOW_SYSTEM_THEME, preferences.themePref)
        assertEquals(setOf(1, 2, 3, 4, 5), preferences.updateDays())
        assertEquals(Time(9, 30), preferences.startTime())
        assertEquals(Time(16, 0), preferences.endTime())
    }

    @Test
    fun updateIntervalMapping() {
        preferences.updateIntervalPref = 0
        assertEquals(5 * 60 * 1000L, preferences.updateIntervalMs)
        preferences.updateIntervalPref = 4
        assertEquals(60 * 60 * 1000L, preferences.updateIntervalMs)
    }

    @Test
    fun togglesRoundTrip() {
        preferences.setRoundToTwoDecimalPlaces(false)
        assertFalse(preferences.roundToTwoDecimalPlaces())
        preferences.setNotificationAlerts(false)
        assertFalse(preferences.notificationAlerts())
        preferences.setTutorialShown(true)
        assertTrue(preferences.tutorialShown())
    }

    @Test
    fun isRefreshingFlowUpdates() {
        assertFalse(preferences.isRefreshing.value)
        preferences.setRefreshing(true)
        assertTrue(preferences.isRefreshing.value)
    }

    @Test
    fun themePrefRoundTrip() {
        preferences.themePref = 1
        assertEquals(1, preferences.themePref)
        preferences.themePref = 0
        assertEquals(0, preferences.themePref)
    }

    @Test
    fun crumbRoundTrip() {
        assertNull(preferences.getCrumb())
        preferences.setCrumb("abc123")
        assertEquals("abc123", preferences.getCrumb())
        preferences.setCrumb(null)
        assertNull(preferences.getCrumb())
    }

    @Test
    fun updateDaysRoundTripAndEmptyFallsBackToDefault() {
        preferences.setUpdateDays(setOf(6, 7))
        assertEquals(setOf(6, 7), preferences.updateDays())
        preferences.setUpdateDays(emptySet())
        assertEquals(setOf(1, 2, 3, 4, 5), preferences.updateDays())
    }
}
