package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.github.premnirmal.ticker.Analytics
import com.github.premnirmal.ticker.RefreshReceiver
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.tickerwidget.R
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Instant
import org.threeten.bp.LocalDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by premnirmal on 2/28/16.
 */
internal class AlarmScheduler {

  companion object {
    val UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE"

    /**
     * Takes care of weekends and afterhours

     * @return
     */
    internal fun msOfNextAlarm(): Long {
      return SystemClock.elapsedRealtime() + msToNextAlarm()
    }

    internal fun msToNextAlarm(): Long {
      val hourOfDay = LocalDateTime.now().hour
      val minuteOfHour = LocalDateTime.now().minute
      val dayOfWeek = LocalDateTime.now().dayOfWeek

      val startTimez = Tools.startTime()
      val endTimez = Tools.endTime()

      run({
        // don't allow end time less than start time. reset to default time if so
        if (endTimez[0] < startTimez[0] || (endTimez[0] == startTimez[0] && endTimez[0] <= startTimez[0])) {
          startTimez[0] = 9
          startTimez[1] = 30
          endTimez[0] = 16
          endTimez[1] = 15
        }
      })

      var mutableDateTime: ZonedDateTime = ZonedDateTime.now()

      var set = false

      if (hourOfDay > endTimez[0] || hourOfDay == endTimez[0] && minuteOfHour > endTimez[1]) {
        mutableDateTime = mutableDateTime.plusDays(1)
            .withHour(startTimez[0])
            .withMinute(startTimez[1])
        set = true
      } else if (dayOfWeek <= DayOfWeek.FRIDAY && (hourOfDay < startTimez[0] || hourOfDay == startTimez[0] && minuteOfHour < startTimez[1])) {
        mutableDateTime = mutableDateTime.withHour(startTimez[0])
            .withMinute(startTimez[1])
        return mutableDateTime.toInstant().toEpochMilli() - ZonedDateTime.now().toInstant().toEpochMilli()
      }

      if (set && dayOfWeek == DayOfWeek.FRIDAY) {
        mutableDateTime = mutableDateTime.plusDays(2)
      }

      if (dayOfWeek > DayOfWeek.FRIDAY) {
        if (dayOfWeek == DayOfWeek.SATURDAY) {
          mutableDateTime = mutableDateTime.plusDays(if (set) 1 else 2)
        } else if (dayOfWeek == DayOfWeek.SUNDAY) {
          if (!set) {
            mutableDateTime = mutableDateTime.plusDays(1)
          }
        }
        if (!set) {
          set = true
          mutableDateTime = mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
        }
      }
      val msToNextAlarm: Long
      if (set) {
        msToNextAlarm = mutableDateTime.toInstant().toEpochMilli() - ZonedDateTime.now().toInstant().toEpochMilli()
      } else {
        msToNextAlarm = Tools.updateInterval
      }
      return msToNextAlarm
    }

    internal fun scheduleUpdate(msToNextAlarm: Long, context: Context): ZonedDateTime {
      val nextAlarm = msToNextAlarm - SystemClock.elapsedRealtime()
      Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
          "UpdateScheduled for " + nextAlarm / (1000 * 60) + " minutes")
      val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
      updateReceiverIntent.action = UPDATE_FILTER
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
          updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      val instant = Instant.ofEpochMilli(System.currentTimeMillis() + nextAlarm)
      val nextAlarmDate = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
      alarmManager.set(AlarmManager.ELAPSED_REALTIME_WAKEUP, msToNextAlarm, pendingIntent)
      return nextAlarmDate
    }

    internal fun sendBroadcast(context: Context) {
      val intent = Intent(context.applicationContext, StockWidget::class.java)
      intent.action = AppWidgetManager.ACTION_APPWIDGET_UPDATE
      val widgetManager = AppWidgetManager.getInstance(context)
      val ids = widgetManager.getAppWidgetIds(ComponentName(context, StockWidget::class.java))
      widgetManager.notifyAppWidgetViewDataChanged(ids, R.id.list)
      intent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS, ids)
      context.sendBroadcast(intent)
    }
  }

}
