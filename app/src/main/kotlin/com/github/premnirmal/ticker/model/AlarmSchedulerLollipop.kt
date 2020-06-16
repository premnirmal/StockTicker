package com.github.premnirmal.ticker.model

import android.app.job.JobInfo
import android.app.job.JobScheduler
import android.content.ComponentName
import android.content.Context
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import timber.log.Timber

@RequiresApi(VERSION_CODES.LOLLIPOP)
internal object AlarmSchedulerLollipop {

  private const val JOB_ID_SCHEDULE = 8424
  private const val FIVE_MINUTES_MS = 5 * 60 * 1000L

  internal fun scheduleUpdate(
    msToNextAlarm: Long,
    context: Context
  ) {
    val jobScheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
    val componentName = ComponentName(context, RefreshService::class.java)
    val builder = JobInfo.Builder(JOB_ID_SCHEDULE, componentName)
    builder.setPersisted(true)
        .setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
        .setRequiresDeviceIdle(false)
        .setRequiresCharging(false)
        .setMinimumLatency(msToNextAlarm)
        .setOverrideDeadline(msToNextAlarm + FIVE_MINUTES_MS)
    if (VERSION.SDK_INT >= VERSION_CODES.O) {
      builder.setRequiresBatteryNotLow(false)
          .setRequiresStorageNotLow(false)
    }
    val jobInfo = builder.build()
    val scheduled = jobScheduler.schedule(jobInfo) == JobScheduler.RESULT_SUCCESS
    if (!scheduled) {
      Timber.e(Exception("Job schedule failed!"))
    }
  }

}