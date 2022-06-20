package com.github.premnirmal.ticker.debug

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.viewModels
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.databinding.ActivityDbViewerBinding

class DbViewerActivity : BaseActivity<ActivityDbViewerBinding>() {

  override val simpleName: String
    get() = "DebugViewerActivity"

  private val viewModel: DbViewerViewModel by viewModels()


  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    binding.toolbar.setNavigationOnClickListener {
      finish()
    }
    binding.webview.settings.allowFileAccess = true

    viewModel.htmlFile.observe(this) {
      binding.webview.loadUrl("file://${it.absolutePath}")
    }

    viewModel.showProgress.observe(this) { show ->
      binding.progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    viewModel.generateDatabaseHtml()
  }
}