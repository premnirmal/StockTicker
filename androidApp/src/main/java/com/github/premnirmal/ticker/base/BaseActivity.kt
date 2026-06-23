package com.github.premnirmal.ticker.base

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import coil3.ImageLoader
import coil3.compose.setSingletonImageLoaderFactory
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import org.koin.androidx.viewmodel.ext.android.viewModel
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.AppMessaging
import com.github.premnirmal.ticker.ui.CollectBottomSheetMessage
import com.github.premnirmal.ticker.ui.LocalAppMessaging
import com.github.premnirmal.ticker.ui.ThemeViewModel
import com.github.premnirmal.tickerwidget.ui.theme.AppTheme
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject

abstract class BaseActivity : ComponentActivity() {

    abstract val simpleName: String
    open val subscribeToErrorEvents = true
    private var isErrorDialogShowing = false

    private val themeViewModel by viewModel<ThemeViewModel>()

    val analytics: Analytics by inject()

    val stocksProvider: StocksProvider by inject()

    val appPreferences: AppPreferences by inject()

    val appMessaging: AppMessaging by inject()

    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(
        savedInstanceState: Bundle?
    ) {
        super.onCreate(savedInstanceState)
        savedInstanceState?.let { isErrorDialogShowing = it.getBoolean(IS_ERROR_DIALOG_SHOWING, false) }
        create(savedInstanceState)
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
            setSingletonImageLoaderFactory { context ->
                ImageLoader.Builder(context)
                    .components {
                        add(OkHttpNetworkFetcherFactory())
                    }
                    .build()
            }
            val currentTheme by themeViewModel.themePref.collectAsStateWithLifecycle(
                initialValue = SelectedTheme.SYSTEM
            )
            AppTheme(theme = currentTheme) {
                CompositionLocalProvider(LocalAppMessaging provides appMessaging) {
                    val isDarkTheme = isSystemInDarkTheme()
                    DisposableEffect(isDarkTheme) {
                        enableEdgeToEdge()
                        onDispose {}
                    }
                    Box {
                        ShowContent()
                        CollectBottomSheetMessage()
                    }
                }
            }
        }
    }

    open fun create(savedInstanceState: Bundle?) {}

    @Composable
    abstract fun ShowContent()

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        analytics.trackScreenView(simpleName)
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
