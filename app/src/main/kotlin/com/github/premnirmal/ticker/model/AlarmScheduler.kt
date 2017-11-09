package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.AppClock
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.widget.RefreshReceiver
import org.threeten.bp.DayOfWeek.FRIDAY
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Created by premnirmal on 2/28/16.
 */
@Singleton
class AlarmScheduler @Inject constructor() {

  @Inject lateinit internal var appPreferences: AppPreferences
  @Inject lateinit internal var clock: AppClock

  init {
    Injector.appComponent.inject(this)
  }

  /**
   * Takes care of weekends and after hours
   */
  fun msToNextAlarm(lastFetchedMs: Long): Long {
    val dayOfWeek = clock.todayLocal().dayOfWeek
    val startTimez = appPreferences.startTime()
    val endTimez = appPreferences.endTime()
    // whether the start time is after the end time e.g. start time is 11pm and end time is 5am
    val inverse = startTimez[0] > endTimez[0] ||
        (startTimez[0] == endTimez[0] && startTimez[1] > endTimez[1])
    val now: ZonedDateTime = clock.todayZoned()
    var mutableDateTime: ZonedDateTime = clock.todayZoned()
    val startTime = clock.todayZoned()
        .withHour(startTimez[0])
        .withMinute(startTimez[1])
    var endTime = clock.todayZoned()
        .withHour(endTimez[0])
        .withMinute(endTimez[1])
    if (inverse && now.isAfter(startTime)) {
      endTime = endTime.plusDays(1)
    }

    val lastFetchedTime = ZonedDateTime.ofInstant(
        Instant.ofEpochMilli(lastFetchedMs), ZoneId.systemDefault())

    val msToNextAlarm: Long
    if (now.isBefore(endTime)
        && (now.isAfter(startTime) || now.isEqual(startTime))
        && dayOfWeek <= FRIDAY) {
      mutableDateTime = if (lastFetchedMs > 0
          && Duration.between(lastFetchedTime, now).toMillis() >= appPreferences.updateIntervalMs) {
        mutableDateTime.plusMinutes(1)
      } else {
        mutableDateTime.plusSeconds(appPreferences.updateIntervalMs / 1000L)
      }
    } else if (!inverse && now.isBefore(startTime) && dayOfWeek <= FRIDAY) {
      mutableDateTime = if (lastFetchedMs > 0 && lastFetchedTime.isBefore(endTime.minusDays(1))) {
        mutableDateTime.plusMinutes(1)
      } else {
        mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
      }
    } else {
      if (dayOfWeek <= FRIDAY && lastFetchedMs > 0 && lastFetchedTime.isBefore(endTime)) {
        mutableDateTime = mutableDateTime.plusMinutes(1)
      } else {
        mutableDateTime = mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
        if (dayOfWeek == FRIDAY) {
          mutableDateTime = mutableDateTime.plusDays(3)
        } else if (dayOfWeek == SATURDAY) {
          mutableDateTime = mutableDateTime.plusDays(2)
        } else if (dayOfWeek == SUNDAY || !inverse) {
          mutableDateTime = mutableDateTime.plusDays(1)
        }
      }
    }
    msToNextAlarm = mutableDateTime.toInstant().toEpochMilli() - now.toInstant().toEpochMilli()
    return msToNextAlarm
  }

  fun scheduleUpdate(msToNextAlarm: Long, context: Context): ZonedDateTime {
    Analytics.INSTANCE.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "UpdateScheduled for " + msToNextAlarm / (1000 * 60) + " minutes")
    Timber.i("Scheduled for " + msToNextAlarm / (1000 * 60) + " minutes")
    val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
    updateReceiverIntent.action = AppPreferences.UPDATE_FILTER
    val instant = Instant.ofEpochMilli(clock.currentTimeMillis() + msToNextAlarm)
    val nextAlarmDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      AlarmSchedulerLollipop.scheduleUpdate(msToNextAlarm, context)
    } else {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
          updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      if (VERSION.SDK_INT >= VERSION_CODES.KITKAT) {
        alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                clock.elapsedRealtime() + msToNextAlarm, pendingIntent)
      } else {
        alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
                clock.elapsedRealtime() + msToNextAlarm, pendingIntent)
      }
    }
    return nextAlarmDate
  }
}
