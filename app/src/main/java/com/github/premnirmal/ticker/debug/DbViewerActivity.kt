package com.github.premnirmal.ticker.debug

import android.os.Bundle
import android.webkit.WebView
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.TopBar
import com.github.premnirmal.tickerwidget.R
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class DbViewerActivity : BaseActivity() {

    override val simpleName = "DebugViewerActivity"
    override val subscribeToErrorEvents = false

    private val viewModel: DbViewerViewModel by viewModels()

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        viewModel.generateDatabaseHtml()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    override fun ShowContent() {
        Scaffold(
            modifier = Modifier.imePadding(),
            topBar = {
                TopBar(
                    text = stringResource(R.string.db_viewer),
                )
            },
            snackbarHost = {
                SnackbarHost(hostState = LocalAppMessaging.current.snackbarHostState)
            }
        ) { paddingValues ->
            Box(Modifier.fillMaxSize().padding(paddingValues)) {
                val showProgress by viewModel.showProgress.collectAsStateWithLifecycle()
                val htmlFile by viewModel.htmlFile.collectAsStateWithLifecycle()
                AndroidView(
                    modifier = Modifier.fillMaxSize(),
                    factory = {
                        WebView(it).apply {
                            settings.allowFileAccess = true
                        }
                    },
                    update = { webView ->
                        htmlFile?.let {
                            webView.loadUrl("file://${it.absolutePath}")
                        }
                    }
                )

                if (showProgress) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        CircularProgressIndicator(
                            modifier = Modifier.align(Alignment.Center)
                        )
                    }
                }
            }
        }
    }
}
