package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.github.premnirmal.ticker.base.BaseComposeActivity
import com.github.premnirmal.ticker.widget.WidgetsScreen
import com.github.premnirmal.tickerwidget.R
import com.google.accompanist.adaptive.calculateDisplayFeatures
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetSettingsActivity : BaseComposeActivity() {

    companion object {
        const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    }

    private var widgetId: Int? = null
    override val simpleName: String = "WidgetSettingsActivity"

    override fun create(savedInstanceState: Bundle?) {
        super.create(savedInstanceState)
        if (intent.hasExtra(ARG_WIDGET_ID)) {
            widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
        }
        if (widgetId != null) {
            setOkResult()
        } else {
            setResult(RESULT_CANCELED)
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
            WidgetsScreen(
                widthSizeClass = windowSizeClass.widthSizeClass,
                displayFeatures = displayFeatures,
                selectedWidgetId = widgetId,
                showSpinner = false,
                topAppBarActions = {
                    IconButton(
                        onClick = {
                            setOkResult()
                            finish()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.done),
                            style = MaterialTheme.typography.labelMedium,
                        )
                    }
                }
            )
        }
    }

    private fun setOkResult() {
        val result = Intent()
        result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
        setResult(RESULT_OK, result)
    }
}
