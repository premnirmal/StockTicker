package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.CustomTabs
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.navigation.calculateContentAndNavigationType
import com.github.premnirmal.ticker.news.QuoteDetailActivity
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.tickerwidget.R.drawable
import com.github.premnirmal.tickerwidget.R.string
import com.google.accompanist.adaptive.FoldAwareConfiguration
import com.google.accompanist.adaptive.HorizontalTwoPaneStrategy
import com.google.accompanist.adaptive.TwoPane
import com.google.accompanist.adaptive.calculateDisplayFeatures

class SearchActivity : BaseActivity() {
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
        val contentType = calculateContentAndNavigationType(
            widthSizeClass = windowSizeClass.widthSizeClass,
            displayFeatures = displayFeatures
        ).second
        Scaffold(
            modifier = Modifier,
            snackbarHost = {
                SnackbarHost(hostState = LocalAppMessaging.current.snackbarHostState)
            }
        ) { paddingValues ->
            val context = LocalContext.current
            val primaryColor = MaterialTheme.colorScheme.primary
            val appMessaging = LocalAppMessaging.current
            val suggestionsErrorText = stringResource(string.error_fetching_suggestions)
            Box(
                modifier = Modifier.background(MaterialTheme.colorScheme.surface),
            ) {
                SearchScreen(
                    modifier = Modifier,
                    contentType = contentType,
                    selectedWidgetId = widgetId,
                    titleText = stringResource(string.action_search),
                    searchLabel = stringResource(string.enter_a_symbol),
                    noDataText = stringResource(string.no_data),
                    suggestionsErrorText = suggestionsErrorText,
                    holdingsLabel = stringResource(string.holdings),
                    dayChangeLabel = stringResource(string.day_change_amount),
                    changePercentLabel = stringResource(string.change_percent),
                    gainLabel = stringResource(string.gain),
                    lossLabel = stringResource(string.loss),
                    changeAmountLabel = stringResource(string.change_amount),
                    clearIcon = painterResource(drawable.ic_close),
                    suggestionAddIcon = painterResource(drawable.ic_add_to_list),
                    onArticleClick = { article ->
                        CustomTabs.openTab(context, article.url, primaryColor.toArgb())
                    },
                    onSuggestionsError = {
                        appMessaging.sendSnackbar(suggestionsErrorText)
                    },
                    twoPane = { first, second ->
                        TwoPane(
                            strategy = HorizontalTwoPaneStrategy(
                                splitFraction = 1f / 2f,
                            ),
                            displayFeatures = displayFeatures,
                            foldAwareConfiguration = FoldAwareConfiguration.VerticalFoldsOnly,
                            first = first,
                            second = second,
                        )
                    },
                    addSymbolDialog = { symbol, onDismissRequest ->
                        AddSymbolDialog(
                            symbol = symbol,
                            onDismissRequest = onDismissRequest,
                        )
                    },
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
}
