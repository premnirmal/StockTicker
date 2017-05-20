package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import com.github.premnirmal.ticker.Analytics
import com.github.premnirmal.ticker.RefreshReceiver
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.tickerwidget.R
import org.threeten.bp.DayOfWeek.FRIDAY
import org.threeten.bp.DayOfWeek.SATURDAY
import org.threeten.bp.DayOfWeek.SUNDAY
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime

/**
 * Created by premnirmal on 2/28/16.
 */
internal class AlarmScheduler {

  companion object {
    val UPDATE_FILTER = "com.github.premnirmal.ticker.UPDATE"

    /**
     * Takes care of weekends and after hours

     * @return
     */
    internal fun msOfNextAlarm(): Long {
      return Tools.clock().elapsedRealtime() + msToNextAlarm()
    }

    internal fun msToNextAlarm(): Long {
      val dayOfWeek = Tools.clock().todayLocal().dayOfWeek

      val startTimez = Tools.startTime()
      val endTimez = Tools.endTime()

      val inverse = startTimez[0] > endTimez[0] ||
          (startTimez[0] == endTimez[0] && startTimez[1] > endTimez[1])

      val now: ZonedDateTime = Tools.clock().todayZoned()
      var mutableDateTime: ZonedDateTime = Tools.clock().todayZoned()

      val startTime = Tools.clock().todayZoned()
          .withHour(startTimez[0])
          .withMinute(startTimez[1])
      var endTime = Tools.clock().todayZoned()
          .withHour(endTimez[0])
          .withMinute(endTimez[1])
      if (inverse && now.isAfter(startTime)) {
        endTime = endTime.plusDays(1)
      }

      val msToNextAlarm: Long
      if (now.isBefore(endTime) && now.isAfter(startTime) && dayOfWeek <= FRIDAY) {
        msToNextAlarm = Tools.updateInterval
      } else if (!inverse && now.isBefore(startTime)) {
        mutableDateTime = mutableDateTime.withHour(startTimez[0]).withMinute(startTimez[1])
        msToNextAlarm = mutableDateTime.toInstant().toEpochMilli() -
            Tools.clock().todayZoned().toInstant().toEpochMilli()
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
            Tools.clock().todayZoned().toInstant().toEpochMilli()
      }
      return msToNextAlarm
    }

    internal fun scheduleUpdate(msToNextAlarm: Long, context: Context): ZonedDateTime {
      val nextAlarm = msToNextAlarm - Tools.clock().elapsedRealtime()
      Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
          "UpdateScheduled for " + nextAlarm / (1000 * 60) + " minutes")
      val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
      updateReceiverIntent.action = UPDATE_FILTER
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
          updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      val instant = Instant.ofEpochMilli(Tools.clock().currentTimeMillis() + nextAlarm)
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
