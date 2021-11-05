package com.sec.android.app.shealth.mock

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

/**
 * Created by android on 3/30/17.
 */
class MockTestRunner : AndroidJUnitRunner() {

  @Throws(InstantiationException::class, IllegalAccessException::class,
      ClassNotFoundException::class) override fun newApplication(cl: ClassLoader, className: String,
    context: Context): Application {
    return super.newApplication(cl, TestApplication::class.java.name, context)
  }
}