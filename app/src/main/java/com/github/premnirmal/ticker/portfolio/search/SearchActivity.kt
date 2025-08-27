package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class SearchActivity : BaseComposeActivity() {
    override val simpleName: String = "SearchActivity"

    companion object {
        const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID

        fun launchIntent(
            context: Context,
            widgetId: Int
        ): Intent {
            val intent = Intent(context, SearchActivity::class.java)
            intent.putExtra(ARG_WIDGET_ID, widgetId)
            return intent
        }
    }

    var widgetId: Int? = null

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        if (intent.hasExtra(ARG_WIDGET_ID)) {
            widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
    }

    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    @Composable
    override fun ShowContent() {
        val windowSizeClass = calculateWindowSizeClass(this)
        val displayFeatures = calculateDisplayFeatures(this)
        Box(
            modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                .systemBarsPadding(),
        ) {
            SearchScreen(
                widthSizeClass = windowSizeClass.widthSizeClass,
                selectedWidgetId = widgetId,
                displayFeatures = displayFeatures,
                onQuoteClick = { quote ->
                    val intent = Intent(this@SearchActivity, QuoteDetailActivity::class.java)
                    intent.putExtra(QuoteDetailActivity.EXTRA_SYMBOL, quote.symbol)
                    intent.putExtra(QuoteDetailActivity.ARG_WIDGET_ID, widgetId)
                    startActivity(intent)
                }
            )
        }
    }
}
