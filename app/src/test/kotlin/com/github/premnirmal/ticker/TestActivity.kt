package com.github.premnirmal.ticker

import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.tickerwidget.databinding.ActivityTestBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class TestActivity : BaseActivity<ActivityTestBinding>() {
	override val binding: (ActivityTestBinding) by viewBinding(ActivityTestBinding::inflate)
  override val simpleName: String = "TestActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
  }
}