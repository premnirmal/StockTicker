package com.github.premnirmal.ticker

import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.mock.MockAppComponent
import com.github.premnirmal.tickerwidget.R

class TestActivity : BaseActivity() {
  override val simpleName: String = "TestActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (Injector.appComponent as MockAppComponent).inject(this)
    setContentView(R.layout.activity_test)
  }
}