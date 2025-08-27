package com.github.premnirmal.ticker.settings

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.github.premnirmal.ticker.base.BaseComposeActivity
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WebViewActivity : BaseComposeActivity() {
    override var simpleName = "WebViewActivity"
    private lateinit var url: String

    override val subscribeToErrorEvents = false

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        url = intent.getStringExtra(EXTRA_URL).orEmpty()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ShowContent() {
        Scaffold(
            modifier = Modifier.imePadding().systemBarsPadding(),
            topBar = {
                Text(
                    modifier = Modifier.padding(16.dp),
                    text = url,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        WebView(it).apply {
                            this.settings.loadsImagesAutomatically = true
                            this.settings.javaScriptEnabled = true
                            this.scrollBarStyle = View.SCROLLBARS_INSIDE_OVERLAY
                            this.webViewClient = WebViewClient()
                        }
                    },
                    update = { webView ->
                        webView.loadUrl(url)
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_URL = "URL"

        fun newIntent(context: Context, url: String): Intent {
            return Intent(context, WebViewActivity::class.java)
                .putExtra(EXTRA_URL, url)
        }
    }
}
