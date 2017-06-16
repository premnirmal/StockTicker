package com.github.premnirmal.ticker.components

import android.content.Context
import com.crashlytics.android.Crashlytics
import com.crashlytics.android.answers.Answers
import com.crashlytics.android.core.CrashlyticsCore
import com.github.premnirmal.tickerwidget.BuildConfig
import io.fabric.sdk.android.Fabric


/**
 * Created by premnirmal on 2/28/16.
 */
internal class CrashLoggerImpl : CrashLogger {

  constructor(context: Context) {
    val kit = Crashlytics.Builder()
        .core(CrashlyticsCore.Builder().disabled(BuildConfig.DEBUG).build())
        .build()
    Fabric.with(context, kit, Answers())
  }

  override fun logException(throwable: Throwable) {
    Crashlytics.logException(throwable)
  }

  override fun log(msg: String) {

  }
}