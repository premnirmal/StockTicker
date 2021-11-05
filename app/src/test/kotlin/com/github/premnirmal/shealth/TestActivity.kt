package com.sec.android.app.shealth

import android.os.Bundle
import com.sec.android.app.shealth.base.BaseActivity
import com.sec.android.app.shealth.components.Injector
import com.sec.android.app.shealth.mock.MockAppComponent
import com.sec.android.app.shealth.R

class TestActivity : BaseActivity() {
  override val simpleName: String = "TestActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    (Injector.appComponent as MockAppComponent).inject(this)
    setContentView(R.layout.activity_test)
  }
}