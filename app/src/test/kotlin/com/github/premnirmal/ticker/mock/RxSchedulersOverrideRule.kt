package com.github.premnirmal.ticker.mock

import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.android.plugins.RxAndroidSchedulersHook
import io.reactivex.functions.Func1
import io.reactivex.plugins.RxJavaHooks
import io.reactivex.schedulers.Schedulers


class RxSchedulersOverrideRule : TestRule {

  private val androidSchedulersHook = object : RxAndroidSchedulersHook() {
    override fun getMainThreadScheduler(): Scheduler {
      return Schedulers.immediate()
    }
  }

  internal val immediateScheduler = Func1<Scheduler, Scheduler> { Schedulers.immediate() }

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        RxAndroidPlugins.getInstance().reset()
        RxAndroidPlugins.getInstance().registerSchedulersHook(androidSchedulersHook)

        RxJavaHooks.reset()
        RxJavaHooks.setOnIOScheduler(immediateScheduler)
        RxJavaHooks.setOnNewThreadScheduler(immediateScheduler)

        base.evaluate()

        RxAndroidPlugins.getInstance().reset()
        RxJavaHooks.reset()
      }
    }
  }

}