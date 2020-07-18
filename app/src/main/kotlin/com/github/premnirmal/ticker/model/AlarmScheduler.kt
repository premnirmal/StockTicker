package com.github.premnirmal.ticker.model

import android.content.Context
import android.content.Intent
import androidx.work.Constraints
import androidx.work.NetworkType.CONNECTED
import androidx.work.OneTimeWorkRequest
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.widget.RefreshReceiver
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import java.util.concurrent.TimeUnit.MILLISECONDS
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class AlarmScheduler {

  @Inject internal lateinit var appPreferences: AppPreferences
  @Inject internal lateinit var clock: AppClock

  init {
    Injector.appComponent.inject(this)
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
    if (now.isBefore(endTime) && (now.isAfter(startTime) || now.isEqual(
            startTime
        )) && selectedDaysOfWeek.contains(dayOfWeek)
    ) {
      nextAlarmDate = if (lastFetchedMs > 0
          && Duration.between(lastFetchedTime,now).toMillis() >= appPreferences.updateIntervalMs
      ) {
        nextAlarmDate.plusMinutes(1)
      } else {
        nextAlarmDate.plusSeconds(appPreferences.updateIntervalMs / 1000L)
      }
    } else if (!inverse && now.isBefore(startTime) && selectedDaysOfWeek.contains(dayOfWeek)) {
      nextAlarmDate = if (lastFetchedMs > 0 && lastFetchedTime.isBefore(endTime.minusDays(1))) {
        nextAlarmDate.plusMinutes(1)
      } else {
        nextAlarmDate.withHour(startTimez.hour)
            .withMinute(startTimez.minute)
      }
    } else {
      if (selectedDaysOfWeek.contains(dayOfWeek) && lastFetchedMs > 0 && lastFetchedTime.isBefore(endTime)) {
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
          Timber.w(Exception("Possible infinite loop in calculating date. Now: ${now.toInstant()}, nextUpdate: ${nextAlarmDate.toInstant()}"))
        }
      }
    }

    val msToNextAlarm = nextAlarmDate.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    return msToNextAlarm
  }

  fun scheduleUpdate(
    msToNextAlarm: Long,
    context: Context
  ): ZonedDateTime {
    Timber.i("Scheduled for ${msToNextAlarm / (1000 * 60)} minutes")
    val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
    updateReceiverIntent.action = AppPreferences.UPDATE_FILTER
    val instant = Instant.ofEpochMilli(clock.currentTimeMillis() + msToNextAlarm)
    val nextAlarmDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    val workRequest: OneTimeWorkRequest = OneTimeWorkRequestBuilder<RefreshWorker>()
        .setConstraints(
            Constraints.Builder()
                .setRequiredNetworkType(CONNECTED)
                .build()
        )
        .addTag(RefreshWorker.TAG)
        .setInitialDelay(msToNextAlarm, MILLISECONDS)
        .build()
    with(WorkManager.getInstance(context)) {
      cancelAllWorkByTag(RefreshWorker.TAG)
      this.enqueue(workRequest)
    }
    return nextAlarmDate
  }
}
