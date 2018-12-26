package com.github.premnirmal.ticker.mock

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Created by premnirmal on 3/30/17.
 */
class MockTestRunner : AndroidJUnitRunner() {

  @Throws(InstantiationException::class, IllegalAccessException::class,
      ClassNotFoundException::class) override fun newApplication(cl: ClassLoader, className: String,
    context: Context): Application {
    return super.newApplication(cl, TestApplication::class.java.name, context)
  }
}