package com.github.premnirmal.ticker.components

import android.util.Log
import com.github.premnirmal.tickerwidget.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import timber.log.Timber

/**
 * Created by premnirmal on 2/28/16.
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
    Log.println(priority, tag, message)
    crashlytics.log(message)
    if (t != null) {
      Log.e(tag, t.message, t)
      crashlytics.recordException(t)
    }
  }
}