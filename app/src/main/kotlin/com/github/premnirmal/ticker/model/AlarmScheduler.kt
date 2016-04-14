package com.github.premnirmal.ticker.model

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.github.premnirmal.ticker.RefreshReceiver
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.ticker.widget.StockWidget
import com.github.premnirmal.tickerwidget.R
import org.joda.time.DateTime
import org.joda.time.DateTimeConstants
import org.joda.time.DateTimeZone
import org.joda.time.MutableDateTime
import com.github.premnirmal.ticker.Analytics

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
      val hourOfDay = DateTime.now().hourOfDay().get()
      val minuteOfHour = DateTime.now().minuteOfHour().get()
      val dayOfWeek = DateTime.now().dayOfWeek

      val startTimez = Tools.startTime()
      val endTimez = Tools.endTime()

      val mutableDateTime = MutableDateTime(DateTime.now())
      mutableDateTime.zone = DateTimeZone.getDefault()

      var set = false

      if (hourOfDay > endTimez[0] || hourOfDay == endTimez[0] && minuteOfHour > endTimez[1]) {
        mutableDateTime.addDays(1)
        mutableDateTime.hourOfDay = startTimez[0]
        mutableDateTime.minuteOfHour = startTimez[1]
        set = true
      } else if (dayOfWeek <= DateTimeConstants.FRIDAY && (hourOfDay < startTimez[0] || hourOfDay == startTimez[0] && minuteOfHour < startTimez[1])) {
        mutableDateTime.hourOfDay = startTimez[0]
        mutableDateTime.minuteOfHour = startTimez[1]
        return mutableDateTime.millis - DateTime.now().millis
      }

      if (set && dayOfWeek == DateTimeConstants.FRIDAY) {
        mutableDateTime.addDays(2)
      }

      if (dayOfWeek > DateTimeConstants.FRIDAY) {
        if (dayOfWeek == DateTimeConstants.SATURDAY) {
          mutableDateTime.addDays(if (set) 1 else 2)
        } else if (dayOfWeek == DateTimeConstants.SUNDAY) {
          if (!set) {
            mutableDateTime.addDays(1)
          }
        }
        if (!set) {
          set = true
          mutableDateTime.hourOfDay = startTimez[0]
          mutableDateTime.minuteOfHour = startTimez[1]
        }
      }
      val msToNextAlarm: Long
      if (set) {
        msToNextAlarm = mutableDateTime.millis - DateTime.now().millis
      } else {
        msToNextAlarm = Tools.updateInterval
      }
      return msToNextAlarm
    }

    internal fun scheduleUpdate(msToNextAlarm: Long, context: Context): DateTime {
      val nextAlarm = msToNextAlarm - SystemClock.elapsedRealtime()
      Analytics.trackUpdate(Analytics.SCHEDULE_UPDATE_ACTION,
          "UpdateScheduled for " + nextAlarm / (1000 * 60) + " minutes")
      val updateReceiverIntent = Intent(context, RefreshReceiver::class.java)
      updateReceiverIntent.action = UPDATE_FILTER
      val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
      val pendingIntent = PendingIntent.getBroadcast(context.applicationContext, 0,
          updateReceiverIntent, PendingIntent.FLAG_UPDATE_CURRENT)
      val nextAlarmDate = DateTime(System.currentTimeMillis() + nextAlarm)
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
