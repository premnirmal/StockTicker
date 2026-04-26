package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.getSystemService
import androidx.work.BackoffPolicy.LINEAR
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.notifications.DailySummaryNotificationReceiver
import com.github.premnirmal.ticker.portfolio.CleanupWorker
import com.github.premnirmal.ticker.widget.RefreshReceiver
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.time.DayOfWeek
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.temporal.ChronoUnit
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.TimeUnit.MINUTES
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class AlarmScheduler @Inject constructor(
    @param:ApplicationContext @field:ApplicationContext
    private val context: Context,
    private val appPreferences: AppPreferences,
    private val clock: AppClock,
    private val workManager: WorkManager,
) {

    fun canScheduleExactAlarm(): Boolean {
        val alarmManager: AlarmManager = context.getSystemService<AlarmManager>() ?: return false
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }
    }

    fun isCurrentTimeWithinScheduledUpdateTime(): Boolean {
        var dayOfWeek = clock.todayLocal()
            .dayOfWeek
        val startTimez = appPreferences.startTime()
        val endTimez = appPreferences.endTime()
        // whether the start time is after the end time e.g. start time is 11pm and end time is 5am
        val inverse =
            startTimez.hour > endTimez.hour || (startTimez.hour == endTimez.hour && startTimez.minute > endTimez.minute)
        val now: ZonedDateTime = clock.todayZoned()
        var startTime = clock.todayZoned()
            .withHour(startTimez.hour)
            .withMinute(startTimez.minute)
        val endTime = clock.todayZoned()
            .withHour(endTimez.hour)
            .withMinute(endTimez.minute)
        if (inverse) {
            if (now.isBefore(startTime)) {
                startTime = startTime.minusDays(1)
            }
        }
        val selectedDaysOfWeek = appPreferences.updateDays()

        if (inverse && now.isBefore(startTime)) {
            var dayOfWeekInt = dayOfWeek.value
            if (dayOfWeekInt == 1) {
                dayOfWeekInt = 7
            } else {
                dayOfWeekInt--
            }
            dayOfWeek = DayOfWeek.of(dayOfWeekInt)
        }
        return now.isBefore(endTime) && (now.isAfter(startTime) || now.isEqual(startTime)) &&
            selectedDaysOfWeek.contains(dayOfWeek)
    }

    /**
     * Takes care of weekends and after hours
     */
    fun msToNextAlarm(lastFetchedMs: Long): Long {
        val dayOfWeek = clock.todayLocal()
            .dayOfWeek
        val startTimez = appPreferences.startTime()
        val endTimez = appPreferences.endTime()
        // whether the start time is after the end time e.g. start time is 11pm and end time is 5am
        val inverse =
            startTimez.hour > endTimez.hour || (startTimez.hour == endTimez.hour && startTimez.minute > endTimez.minute)
        val now: ZonedDateTime = clock.todayZoned()
        val startTime = clock.todayZoned()
            .withHour(startTimez.hour)
            .withMinute(startTimez.minute)
        var endTime = clock.todayZoned()
            .withHour(endTimez.hour)
            .withMinute(endTimez.minute)
        if (inverse && now.isAfter(startTime)) {
            endTime = endTime.plusDays(1)
        }
        val selectedDaysOfWeek = appPreferences.updateDays()
        val lastFetchedTime =
            ZonedDateTime.ofInstant(Instant.ofEpochMilli(lastFetchedMs), ZoneId.systemDefault())

        var nextAlarmDate: ZonedDateTime = clock.todayZoned()
        if (now.isBefore(endTime) && (
                now.isAfter(startTime) || now.isEqual(
                    startTime
                )
                ) && selectedDaysOfWeek.contains(dayOfWeek)
        ) {
            nextAlarmDate = if (lastFetchedMs > 0 &&
                Duration.between(lastFetchedTime, now)
                    .toMillis() >= appPreferences.updateIntervalMs
            ) {
                nextAlarmDate.plusMinutes(1)
            } else {
                nextAlarmDate.plus(appPreferences.updateIntervalMs, ChronoUnit.MILLIS)
            }
        } else if (!inverse && now.isBefore(startTime) && selectedDaysOfWeek.contains(dayOfWeek)) {
            nextAlarmDate = if (lastFetchedMs > 0 && lastFetchedTime.isBefore(endTime.minusDays(1))) {
                nextAlarmDate.plusMinutes(1)
            } else {
                nextAlarmDate.withHour(startTimez.hour)
                    .withMinute(startTimez.minute)
            }
        } else {
            if (selectedDaysOfWeek.contains(dayOfWeek) && lastFetchedMs > 0 && lastFetchedTime.isBefore(
                    endTime
                )
            ) {
                nextAlarmDate = nextAlarmDate.plusMinutes(1)
            } else {
                nextAlarmDate = nextAlarmDate.withHour(startTimez.hour)
                    .withMinute(startTimez.minute)

                var count = 0
                if (inverse) {
                    while (!selectedDaysOfWeek.contains(nextAlarmDate.dayOfWeek) && count <= 7) {
                        count++
                        nextAlarmDate = nextAlarmDate.plusDays(1)
                    }
                } else {
                    do {
                        count++
                        nextAlarmDate = nextAlarmDate.plusDays(1)
                    } while (!selectedDaysOfWeek.contains(nextAlarmDate.dayOfWeek) && count <= 7)
                }

                if (count >= 7) {
                    Timber.w(
                        Exception(
                            "Possible infinite loop in calculating date. Now: ${now.toInstant()}, nextUpdate: ${nextAlarmDate.toInstant()}"
                        )
                    )
                }
            }
        }

        return nextAlarmDate.toInstant()
            .toEpochMilli() - now.toInstant()
            .toEpochMilli()
    }

    fun scheduleUpdate(
        msToNextAlarm: Long,
        context: Context
    ): ZonedDateTime {
        Timber.d("Scheduled for ${msToNextAlarm / (1000 * 60)} minutes")
        val instant = Instant.ofEpochMilli(clock.currentTimeMillis() + msToNextAlarm)
        val nextAlarmDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
        val refreshReceiverIntent = Intent(context, RefreshReceiver::class.java)
        val alarmManager = checkNotNull(context.getSystemService<AlarmManager>())
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            0,
            refreshReceiverIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
        )
        if (canScheduleExactAlarm()) {
            alarmManager.setExact(
                AlarmManager.ELAPSED_REALTIME, clock.elapsedRealtime() + msToNextAlarm, pendingIntent
            )
        } else {
            alarmManager.setWindow(
                AlarmManager.ELAPSED_REALTIME, clock.elapsedRealtime() + msToNextAlarm, MINUTES.toMillis(10), pendingIntent
            )
        }
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .build()
        val request = OneTimeWorkRequestBuilder<RefreshWorker>()
            .setInitialDelay(msToNextAlarm, MILLISECONDS)
            .addTag(RefreshWorker.TAG)
            .setBackoffCriteria(LINEAR, 1L, MINUTES)
            .setConstraints(constraints)
            .build()
        workManager.enqueueUniqueWork(RefreshWorker.TAG, ExistingWorkPolicy.REPLACE, request)

        return nextAlarmDate
    }

    fun enqueuePeriodicRefresh() {
        with(workManager) {
            val delayMs = appPreferences.updateIntervalMs
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()
            val request = PeriodicWorkRequestBuilder<RefreshWorker>(delayMs, MILLISECONDS)
                .setInitialDelay(delayMs, MILLISECONDS)
                .addTag(RefreshWorker.TAG_PERIODIC)
                .setBackoffCriteria(LINEAR, 1L, MINUTES)
                .setConstraints(constraints)
                .build()
            this.enqueueUniquePeriodicWork(RefreshWorker.TAG_PERIODIC, ExistingPeriodicWorkPolicy.REPLACE, request)
        }
    }

    fun enqueuePeriodicCleanup() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        val request = PeriodicWorkRequestBuilder<CleanupWorker>(1, TimeUnit.DAYS)
                .addTag(CleanupWorker.TAG_PERIODIC)
                .setConstraints(constraints)
                .build()
        workManager.enqueueUniquePeriodicWork(CleanupWorker.TAG_PERIODIC, ExistingPeriodicWorkPolicy.UPDATE, request)
    }

    fun enqueueCleanup() {
        val constraints = Constraints.Builder().setRequiredNetworkType(NetworkType.NOT_REQUIRED).build()
        val request = OneTimeWorkRequestBuilder<CleanupWorker>()
            .addTag(CleanupWorker.TAG)
            .setConstraints(constraints).build()
        workManager.enqueueUniqueWork(CleanupWorker.TAG, ExistingWorkPolicy.REPLACE, request)
    }

    fun scheduleDailySummaryNotification(
        context: Context,
        initialDelay: Long,
        interval: Long
    ) {
        Timber.d("enqueueDailySummaryNotification delay:${initialDelay}ms")
        val receiverIntent = Intent(context, DailySummaryNotificationReceiver::class.java)
        val alarmManager = checkNotNull(context.getSystemService<AlarmManager>())
        val pendingIntent =
            PendingIntent.getBroadcast(
                context,
                REQUEST_CODE_SUMMARY_NOTIFICATION,
                receiverIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_ONE_SHOT
            )
        alarmManager.setRepeating(
            AlarmManager.ELAPSED_REALTIME,
            clock.elapsedRealtime() + initialDelay,
            interval,
            pendingIntent
        )
    }

    companion object {
        private const val REQUEST_CODE_SUMMARY_NOTIFICATION = 123
    }
}
