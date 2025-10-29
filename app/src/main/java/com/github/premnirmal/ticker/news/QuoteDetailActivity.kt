package com.github.premnirmal.ticker.news

import android.appwidget.AppWidgetManager
import android.os.Bundle
import androidx.activity.viewModels
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.detail.QuoteDetailScreen
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class QuoteDetailActivity : BaseComposeActivity() {

    override val simpleName: String
        get() = "QuoteDetailActivity"

    private val viewModel: QuoteDetailViewModel by viewModels()
    private var widgetId: Int? = null

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        val symbol = intent.getStringExtra(EXTRA_SYMBOL)
        if (intent.hasExtra(ARG_WIDGET_ID)) {
            widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        if (symbol.isNullOrEmpty()) {
            finish()
            return
        } else {
            viewModel.loadQuote(symbol)
            viewModel.fetchQuote(symbol)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    override fun ShowContent() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val quote by viewModel.quote.collectAsStateWithLifecycle(null)
        if (quote == null) {
            Box(modifier = Modifier.fillMaxSize()) {
                CircularProgressIndicator(
                    modifier = Modifier.align(Alignment.Center)
                )
            }
        } else {
            if (quote?.dataSafe?.quote != null) {
                quote?.dataSafe?.quote?.let {
                    QuoteDetailScreen(
                        widthSizeClass = windowSizeClass.widthSizeClass,
                        contentType = null,
                        displayFeatures = calculateDisplayFeatures(this),
                        quote = it,
                    )
                }
            } else {
                Box(modifier = Modifier.fillMaxSize()) {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }

    companion object {
        const val EXTRA_SYMBOL: String = "EXTRA_SYMBOL"
        const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    }
}
