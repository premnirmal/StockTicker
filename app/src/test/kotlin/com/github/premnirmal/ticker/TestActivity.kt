package com.github.premnirmal.ticker

import android.os.Bundle
import android.view.LayoutInflater
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.mock.MockAppComponent
import com.github.premnirmal.tickerwidget.databinding.ActivityTestBinding

class TestActivity : BaseActivity<ActivityTestBinding>() {
  override val simpleName: String = "TestActivity"


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (Injector.appComponent as MockAppComponent).inject(this)
  }
}