package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebViewClient
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.databinding.ActivityWebviewBinding
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewActivity : BaseActivity<ActivityWebviewBinding>() {
  override val binding by viewBinding(ActivityWebviewBinding::inflate)
  override var simpleName = "WebViewActivity"
  private lateinit var url: String

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    url = intent.getStringExtra(EXTRA_URL).orEmpty()
    binding.toolbar.setNavigationOnClickListener {
      finish()
    }
    binding.urlText.text = url
    binding.webview.apply {
      this.settings.loadsImagesAutomatically = true
      this.settings.javaScriptEnabled = true
      this.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
      this.webViewClient = WebViewClient()
    }
    binding.webview.loadUrl(url)
  }

  companion object {
    private const val EXTRA_URL = "URL"

    fun newIntent(context: Context, url: String): Intent {
      return Intent(context, WebViewActivity::class.java)
        .putExtra(EXTRA_URL, url)
    }
  }
}