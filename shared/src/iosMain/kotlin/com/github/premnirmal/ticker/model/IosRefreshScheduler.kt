package com.github.premnirmal.ticker.model

import com.github.premnirmal.ticker.IosUserPreferences
import com.github.premnirmal.ticker.components.AppClock
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.isoDayNumber
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime

/**
 * iOS implementation of the shared [RefreshScheduler] contract.
 *
 * It is the iOS counterpart of Android's `AlarmScheduler`: it owns the same platform-neutral
 * update-window decisions ([isCurrentTimeWithinScheduledUpdateTime], [msToNextAlarm]) — a faithful
 * port of the Android calendar arithmetic, expressed with `kotlinx-datetime` instead of `java.time`
 * — while delegating the actual background submission to the platform [IosBackgroundTaskScheduler]
 * (a `BGTaskScheduler`/WidgetKit bridge the iOS app provides).
 *
 * iOS has no "exact alarm" permission concept, so [canScheduleExactAlarm] always returns `true`.
 */
class IosRefreshScheduler(
    private val preferences: IosUserPreferences,
    private val clock: AppClock,
    private val backgroundTaskScheduler: IosBackgroundTaskScheduler = NoopIosBackgroundTaskScheduler,
    private val timeZone: TimeZone = TimeZone.currentSystemDefault()
) : RefreshScheduler {

    override fun canScheduleExactAlarm(): Boolean = true

    override fun isCurrentTimeWithinScheduledUpdateTime(): Boolean {
        val nowInstant = nowInstant()
        val now = nowInstant.local()
        var dayOfWeek = now.dayOfWeek.isoDayNumber
        val startTimez = preferences.startTime()
        val endTimez = preferences.endTime()
        val inverse = isInverse(startTimez, endTimez)
        var startTime = now.withTime(startTimez.hour, startTimez.minute)
        val endTime = now.withTime(endTimez.hour, endTimez.minute)
        if (inverse && nowInstant < startTime.instant()) {
            startTime = startTime.plusDays(-1)
        }
        val selectedDaysOfWeek = preferences.updateDays()
        if (inverse && nowInstant < startTime.instant()) {
            dayOfWeek = if (dayOfWeek == 1) 7 else dayOfWeek - 1
        }
        return nowInstant < endTime.instant() &&
            nowInstant >= startTime.instant() &&
            selectedDaysOfWeek.contains(dayOfWeek)
    }

    /**
     * Takes care of weekends and after hours. Mirrors `AlarmScheduler.msToNextAlarm`.
     */
    override fun msToNextAlarm(lastFetchedMs: Long): Long {
        val nowInstant = nowInstant()
        val now = nowInstant.local()
        val dayOfWeek = now.dayOfWeek.isoDayNumber
        val startTimez = preferences.startTime()
        val endTimez = preferences.endTime()
        val inverse = isInverse(startTimez, endTimez)
        val startTime = now.withTime(startTimez.hour, startTimez.minute)
        var endTime = now.withTime(endTimez.hour, endTimez.minute)
        if (inverse && nowInstant > startTime.instant()) {
            endTime = endTime.plusDays(1)
        }
        val selectedDaysOfWeek = preferences.updateDays()
        val lastFetchedInstant = Instant.fromEpochMilliseconds(lastFetchedMs)
        val updateIntervalMs = preferences.updateIntervalMs

        var nextAlarmDate: LocalDateTime = now
        if (nowInstant < endTime.instant() &&
            nowInstant >= startTime.instant() &&
            selectedDaysOfWeek.contains(dayOfWeek)
        ) {
            nextAlarmDate = if (lastFetchedMs > 0 &&
                (nowInstant.toEpochMilliseconds() - lastFetchedInstant.toEpochMilliseconds()) >= updateIntervalMs
            ) {
                now.plusMinutes(1)
            } else {
                now.plusMillis(updateIntervalMs)
            }
        } else if (!inverse && nowInstant < startTime.instant() && selectedDaysOfWeek.contains(dayOfWeek)) {
            nextAlarmDate = if (lastFetchedMs > 0 && lastFetchedInstant < endTime.plusDays(-1).instant()) {
                now.plusMinutes(1)
            } else {
                now.withTime(startTimez.hour, startTimez.minute)
            }
        } else {
            if (selectedDaysOfWeek.contains(dayOfWeek) && lastFetchedMs > 0 &&
                lastFetchedInstant < endTime.instant()
            ) {
                nextAlarmDate = now.plusMinutes(1)
            } else {
                nextAlarmDate = now.withTime(startTimez.hour, startTimez.minute)
                var count = 0
                if (inverse) {
                    while (!selectedDaysOfWeek.contains(nextAlarmDate.dayOfWeek.isoDayNumber) && count <= 7) {
                        count++
                        nextAlarmDate = nextAlarmDate.plusDays(1)
                    }
                } else {
                    do {
                        count++
                        nextAlarmDate = nextAlarmDate.plusDays(1)
                    } while (!selectedDaysOfWeek.contains(nextAlarmDate.dayOfWeek.isoDayNumber) && count <= 7)
                }
            }
        }

        return nextAlarmDate.instant().toEpochMilliseconds() - nowInstant.toEpochMilliseconds()
    }

    override fun enqueuePeriodicRefresh() =
        backgroundTaskScheduler.enqueuePeriodicRefresh(preferences.updateIntervalMs)

    override fun enqueuePeriodicCleanup() = backgroundTaskScheduler.enqueuePeriodicCleanup()

    override fun enqueueCleanup() = backgroundTaskScheduler.enqueueCleanup()

    /** Submits an exact-ish background refresh [msToNextAlarm] from now via the platform bridge. */
    fun scheduleRefresh(msToNextAlarm: Long) = backgroundTaskScheduler.scheduleRefresh(msToNextAlarm)

    private fun nowInstant(): Instant = Instant.fromEpochMilliseconds(clock.currentTimeMillis())

    private fun isInverse(start: IosUserPreferences.Time, end: IosUserPreferences.Time): Boolean =
        start.hour > end.hour || (start.hour == end.hour && start.minute > end.minute)

    private fun Instant.local(): LocalDateTime = toLocalDateTime(timeZone)

    private fun LocalDateTime.instant(): Instant = toInstant(timeZone)

    private fun LocalDateTime.withTime(hour: Int, minute: Int): LocalDateTime =
        LocalDateTime(date, LocalTime(hour, minute, second, nanosecond))

    private fun LocalDateTime.plusDays(days: Int): LocalDateTime =
        LocalDateTime(date.plus(DatePeriod(days = days)), time)

    private fun LocalDateTime.plusMinutes(minutes: Int): LocalDateTime =
        instant().plus(minutes * MILLIS_PER_MINUTE, kotlinx.datetime.DateTimeUnit.MILLISECOND).local()

    private fun LocalDateTime.plusMillis(millis: Long): LocalDateTime =
        instant().plus(millis, kotlinx.datetime.DateTimeUnit.MILLISECOND).local()

    private companion object {
        const val MILLIS_PER_MINUTE = 60_000L
    }
}
