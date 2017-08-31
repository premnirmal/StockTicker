package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.components.Analytics
import com.github.premnirmal.ticker.components.ILogIt
import com.github.premnirmal.ticker.widget.RefreshReceiver
import org.threeten.bp.DayOfWeek.FRIDAY
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by premnirmal on 2/28/16.
 */
object AlarmScheduler {

  /**
   * Takes care of weekends and after hours
   */
  internal fun msToNextAlarm(): Long {
    val dayOfWeek = AppPreferences.clock().todayLocal().dayOfWeek

    val startTimez = AppPreferences.startTime()
    val endTimez = AppPreferences.endTime()

    val inverse = startTimez[0] > endTimez[0] ||
        (startTimez[0] == endTimez[0] && startTimez[1] > endTimez[1])

    val now: ZonedDateTime = AppPreferences.clock().todayZoned()
    var mutableDateTime: ZonedDateTime = AppPreferences.clock().todayZoned()

    val startTime = AppPreferences.clock().todayZoned()
        .withHour(startTimez[0])
        .withMinute(startTimez[1])
    var endTime = AppPreferences.clock().todayZoned()
        .withHour(endTimez[0])
        .withMinute(endTimez[1])
    if (inverse && now.isAfter(startTime)) {
      endTime = endTime.plusDays(1)
    }

    val msToNextAlarm: Long
    if (now.isBefore(endTime) && now.isAfter(startTime) && dayOfWeek <= FRIDAY) {
      msToNextAlarm = AppPreferences.updateInterval
    } else if (!inverse && now.isBefore(startTime) && dayOfWeek <= FRIDAY) {
      mutableDateTime = mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
      msToNextAlarm = mutableDateTime.toInstant().toEpochMilli() -
          now.toInstant().toEpochMilli()
    } else {
      mutableDateTime = mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
      if (dayOfWeek == FRIDAY) {
        mutableDateTime = mutableDateTime.plusDays(3)
      } else if (dayOfWeek == SATURDAY) {
        mutableDateTime = mutableDateTime.plusDays(2)
      } else if (dayOfWeek == SUNDAY || !inverse) {
        mutableDateTime = mutableDateTime.plusDays(1)
      }
      msToNextAlarm = mutableDateTime.toInstant().toEpochMilli() -
          now.toInstant().toEpochMilli()
    }
    return msToNextAlarm
  }

  internal fun scheduleUpdate(msToNextAlarm: Long, context: Context): ZonedDateTime {
    Analytics.INSTANCE.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
        "UpdateScheduled for " + msToNextAlarm / (1000 * 60) + " minutes")
    ILogIt.INSTANCE.log("Scheduled for " + msToNextAlarm / (1000 * 60) + " minutes")
    val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
    updateReceiverIntent.action = AppPreferences.UPDATE_FILTER
    val instant = Instant.ofEpochMilli(AppPreferences.clock().currentTimeMillis() + msToNextAlarm)
    val nextAlarmDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      AlarmSchedulerLollipop.scheduleUpdate(msToNextAlarm, context)
    } else {
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
          updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP,
          AppPreferences.clock().elapsedRealtime() + msToNextAlarm, pendingIntent)
    }
    return nextAlarmDate
  }
}
