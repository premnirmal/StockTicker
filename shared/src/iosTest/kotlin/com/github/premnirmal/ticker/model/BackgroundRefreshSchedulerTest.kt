package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.FakeAppClock
import com.github.premnirmal.ticker.UserDefaultsPreferences
import com.github.premnirmal.ticker.settings.SettingsStore
import kotlinx.datetime.TimeZone
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class BackgroundRefreshSchedulerTest {

    private val suiteName = "test-scheduler-${kotlin.random.Random.nextInt()}"
    private val defaults = NSUserDefaults(suiteName = suiteName)
    private val store = SettingsStore(defaults)
    private val preferences = UserDefaultsPreferences(store)

    @AfterTest
    fun tearDown() {
        defaults.removePersistentDomainForName(suiteName)
    }

    private fun scheduler(
        nowMillis: Long,
        recorder: RecordingBackgroundTaskScheduler = RecordingBackgroundTaskScheduler()
    ) = BackgroundRefreshScheduler(
        preferences = preferences,
        clock = FakeAppClock(nowMillis),
        backgroundTaskScheduler = recorder,
        timeZone = TimeZone.UTC
    )

    private fun configureWindow() {
        preferences.setStartTime("09:30")
        preferences.setEndTime("16:00")
        preferences.setUpdateDays(setOf(1, 2, 3, 4, 5))
        preferences.updateIntervalPref = 1 // 15 minutes
    }

    @Test
    fun withinWindowOnWeekday() {
        configureWindow()
        // Wednesday 2024-01-03 12:00:00 UTC
        val scheduler = scheduler(WED_NOON_UTC)
        assertTrue(scheduler.isCurrentTimeWithinScheduledUpdateTime())
    }

    @Test
    fun outsideWindowAfterHours() {
        configureWindow()
        // Wednesday 2024-01-03 20:00:00 UTC (after 16:00)
        val scheduler = scheduler(WED_NOON_UTC + 8 * HOUR_MS)
        assertFalse(scheduler.isCurrentTimeWithinScheduledUpdateTime())
    }

    @Test
    fun msToNextAlarmInWindowIsUpdateInterval() {
        configureWindow()
        val scheduler = scheduler(WED_NOON_UTC)
        // In-window with no prior fetch -> schedule one update interval (15 min) from now.
        assertEquals(15 * MINUTE_MS, scheduler.msToNextAlarm(lastFetchedMs = 0L))
    }

    @Test
    fun msToNextAlarmAfterHoursJumpsToNextDayStart() {
        configureWindow()
        // Wednesday 20:00 UTC -> next alarm Thursday 09:30 UTC = 13h30m later.
        val scheduler = scheduler(WED_NOON_UTC + 8 * HOUR_MS)
        assertEquals(13 * HOUR_MS + 30 * MINUTE_MS, scheduler.msToNextAlarm(lastFetchedMs = 0L))
    }

    @Test
    fun enqueueDelegatesToBackgroundScheduler() {
        configureWindow()
        val recorder = RecordingBackgroundTaskScheduler()
        val scheduler = scheduler(WED_NOON_UTC, recorder)
        scheduler.enqueuePeriodicRefresh()
        scheduler.enqueuePeriodicCleanup()
        scheduler.enqueueCleanup()
        scheduler.scheduleRefresh(1234L)
        assertEquals(1, recorder.periodicRefreshCount)
        assertEquals(1, recorder.periodicCleanupCount)
        assertEquals(1, recorder.cleanupCount)
        assertEquals(listOf(1234L), recorder.scheduledDelays)
    }

    @Test
    fun canScheduleExactAlarmAlwaysTrue() {
        assertTrue(scheduler(WED_NOON_UTC).canScheduleExactAlarm())
    }

    private companion object {
        const val HOUR_MS = 60L * 60L * 1000L
        const val MINUTE_MS = 60L * 1000L
        // Wednesday 2024-01-03 12:00:00 UTC
        const val WED_NOON_UTC = 1704283200000L
    }
}

class RecordingBackgroundTaskScheduler : BackgroundTaskScheduler {
    var periodicRefreshCount = 0
    var periodicCleanupCount = 0
    var cleanupCount = 0
    val scheduledDelays = mutableListOf<Long>()

    override fun scheduleRefresh(delayMs: Long) {
        scheduledDelays.add(delayMs)
    }

    override fun enqueuePeriodicRefresh(intervalMs: Long) {
        periodicRefreshCount++
    }

    override fun enqueuePeriodicCleanup() {
        periodicCleanupCount++
    }

    override fun enqueueCleanup() {
        cleanupCount++
    }
}
