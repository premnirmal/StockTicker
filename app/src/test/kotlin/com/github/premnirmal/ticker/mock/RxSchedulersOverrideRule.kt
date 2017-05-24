package com.github.premnirmal.ticker.mock

import io.reactivex.Scheduler
import io.reactivex.android.plugins.RxAndroidPlugins
import io.reactivex.internal.schedulers.ExecutorScheduler
import io.reactivex.plugins.RxJavaPlugins
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement
import java.util.concurrent.Executor


class RxSchedulersOverrideRule : TestRule {

  private val immediate = object : Scheduler() {

    override fun createWorker(): Worker {
      return ExecutorScheduler.ExecutorWorker(Executor { it.run() })
    }
  }

  override fun apply(base: Statement, description: Description): Statement {
    return object : Statement() {
      @Throws(Throwable::class)
      override fun evaluate() {
        RxJavaPlugins.setInitIoSchedulerHandler { _ -> immediate }
        RxJavaPlugins.setInitComputationSchedulerHandler { _ -> immediate }
        RxJavaPlugins.setInitNewThreadSchedulerHandler { _ -> immediate }
        RxJavaPlugins.setInitSingleSchedulerHandler { _ -> immediate }
        RxAndroidPlugins.setInitMainThreadSchedulerHandler { _ -> immediate }

        try {
          base.evaluate()
        } finally {
          RxJavaPlugins.reset()
          RxAndroidPlugins.reset()
        }
      }
    }
  }

}