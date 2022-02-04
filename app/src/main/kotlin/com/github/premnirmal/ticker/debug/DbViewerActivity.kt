package com.github.premnirmal.ticker.debug

import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_db_viewer.progress
import kotlinx.android.synthetic.main.activity_db_viewer.toolbar
import kotlinx.android.synthetic.main.activity_db_viewer.webview

class DbViewerActivity : BaseActivity() {

  override val simpleName: String
    get() = "DebugViewerActivity"

  private val viewModel: DbViewerViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    setContentView(R.layout.activity_db_viewer)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    webview.settings.allowFileAccess = true

    viewModel.htmlFile.observe(this) {
      webview.loadUrl("file://${it.absolutePath}")
    }

    viewModel.showProgress.observe(this) { show ->
      progress.visibility = if (show) View.VISIBLE else View.GONE
    }

    viewModel.generateDatabaseHtml()
  }
}