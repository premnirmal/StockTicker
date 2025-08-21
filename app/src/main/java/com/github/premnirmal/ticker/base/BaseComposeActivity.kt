package com.github.premnirmal.ticker.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.ui.CollectBottomSheetMessage
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.ShowSnackBar
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseComposeActivity : ComponentActivity() {

    abstract val simpleName: String
    open val subscribeToErrorEvents = true
    private var isErrorDialogShowing = false

    @Inject lateinit var analytics: Analytics

    @Inject lateinit var stocksProvider: StocksProvider

    @Inject lateinit var appPreferences: AppPreferences

    @Inject lateinit var appMessaging: AppMessaging

    @Composable
    protected fun ApplyThemeColourToNavigationBar() {
        window.navigationBarColor = colorScheme.primary
            .copy(alpha = 0.08f)
            .compositeOver(colorScheme.surface.copy())
            .toArgb()
    }

    @Composable
    protected fun ApplyThemeColourToStatusBar() {
        window.statusBarColor = colorScheme.surface.toArgb()
    }

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        DynamicColors.applyToActivityIfAvailable(this)
        savedInstanceState?.let { isErrorDialogShowing = it.getBoolean(IS_ERROR_DIALOG_SHOWING, false) }
        if (subscribeToErrorEvents) {
            lifecycleScope.launch {
                repeatOnLifecycle(Lifecycle.State.STARTED) {
                    stocksProvider.fetchState.collect { state ->
                        if (state is FetchState.Failure) {
                            if (this.isActive && !isErrorDialogShowing && !isFinishing) {
                                isErrorDialogShowing = true
                                showDialog(state.displayString).setOnDismissListener { isErrorDialogShowing = false }
                                delay(500L)
                            }
                        }
                    }
                }
            }
        }
        setContent {
            AppTheme {
                CompositionLocalProvider(LocalAppMessaging provides appMessaging) {
                    ApplyThemeColourToNavigationBar()
                    ApplyThemeColourToStatusBar()
                    ShowContent()

                    CollectBottomSheetMessage()
                    ShowSnackBar()
                }
            }
        }
    }

    @Composable
    abstract fun ShowContent()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        analytics.trackScreenView(simpleName, this)
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean(IS_ERROR_DIALOG_SHOWING, isErrorDialogShowing)
    }

    companion object {
        private const val IS_ERROR_DIALOG_SHOWING = "IS_ERROR_DIALOG_SHOWING"
    }
}
