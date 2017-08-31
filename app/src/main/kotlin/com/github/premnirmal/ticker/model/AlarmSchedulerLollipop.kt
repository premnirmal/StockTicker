package com.github.premnirmal.ticker.model

import android.annotation.TargetApi
import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build.VERSION_CODES
import android.support.annotation.RequiresApi

@RequiresApi(VERSION_CODES.LOLLIPOP)
@TargetApi(VERSION_CODES.LOLLIPOP)
internal object AlarmSchedulerLollipop {

  private val JOB_ID_SCHEDULE = 8424
  private val HALF_MINUTE_MS = 30 * 1000L

  internal fun scheduleUpdate(msToNextAlarm: Long, context: Context) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val componentName = ComponentName(context, RefreshService::class.java)
    jobScheduler.cancel(JOB_ID_SCHEDULE)
    val jobInfo = JobInfo.Builder(JOB_ID_SCHEDULE, componentName)
        .setPersisted(true)
        .setMinimumLatency(msToNextAlarm)
        .setOverrideDeadline(msToNextAlarm + HALF_MINUTE_MS)
        .build()
    jobScheduler.schedule(jobInfo)
  }

}