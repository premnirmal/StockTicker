package com.github.premnirmal.ticker.components

import android.content.Context
import android.util.Log
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.github.premnirmal.tickerwidget.BuildConfig
import io.fabric.sdk.android.Fabric
import timber.log.Timber

/**
 * Created by premnirmal on 2/28/16.
 */
class LoggingTree(context: Context) : Timber.Tree() {

  init {
    val kit = Crashlytics.Builder()
        .core(
            CrashlyticsCore.Builder()
                .disabled(BuildConfig.DEBUG)
                .build()
        )
        .build()
    Fabric.with(context, kit, Answers())
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
    if (message != null) {
      Crashlytics.log(message)
    }
    if (t != null) {
      Crashlytics.logException(t)
    }
  }
}