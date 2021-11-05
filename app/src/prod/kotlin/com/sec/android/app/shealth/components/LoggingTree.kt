package com.sec.android.app.shealth.components

import android.util.Log
import com.sec.android.app.shealth.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Created by android on 2/28/16.
 */
class LoggingTree : Timber.Tree() {

  private val crashlytics: FirebaseCrashlytics = FirebaseCrashlytics.getInstance().apply {
    setCrashlyticsCollectionEnabled(!BuildConfig.DEBUG)
  }

  override fun log(
    priority: Int,
    tag: String?,
    message: String,
    t: Throwable?
  ) {
    if (priority == Log.VERBOSE || priority == Log.DEBUG) {
      return
    }
    crashlytics.log(message)
    if (t != null) {
      crashlytics.recordException(t)
    }
  }
}