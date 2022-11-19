package com.github.premnirmal.ticker.base

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.material3.MaterialTheme.colorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.ui.theme.SelectedTheme
import com.google.android.material.color.DynamicColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

abstract class BaseComposeActivityForFragments : AppCompatActivity() {

  abstract val simpleName: String
  open val subscribeToErrorEvents = true
  private var isErrorDialogShowing = false
  @Inject lateinit var analytics: Analytics
  @Inject lateinit var stocksProvider: StocksProvider
  @Inject lateinit var appPreferences: AppPreferences

  protected val currentTheme: SelectedTheme
    get() = when (appPreferences.themePref) {
      AppPreferences.JUST_BLACK_THEME -> SelectedTheme.JUST_BLACK
      AppPreferences.DARK_THEME -> SelectedTheme.DARK
      AppPreferences.LIGHT_THEME -> SelectedTheme.LIGHT
      AppPreferences.FOLLOW_SYSTEM_THEME -> SelectedTheme.SYSTEM
      else -> SelectedTheme.SYSTEM
    }

  @Composable
  protected fun ApplyThemeColourToNavigationBar() {
    window.navigationBarColor = colorScheme.primary
        .copy(alpha = 0.08f)
        .compositeOver(colorScheme.surface.copy())
        .toArgb()
  }

  @Composable
  protected fun ApplyThemeColourToStatusBar() {
    window.statusBarColor = colorScheme.primary
        .copy(alpha = 0.08f)
        .compositeOver(colorScheme.surface.copy())
        .toArgb()
  }

  override fun onCreate(
    savedInstanceState: Bundle?
  ) {
    super.onCreate(savedInstanceState)
    DynamicColors.applyToActivityIfAvailable(this)
    savedInstanceState?.let { isErrorDialogShowing = it.getBoolean(IS_ERROR_DIALOG_SHOWING, false) }
  }

  override fun onPostCreate(savedInstanceState: Bundle?) {
    super.onPostCreate(savedInstanceState)
    analytics.trackScreenView(simpleName, this)
  }

  override fun onResume() {
    super.onResume()
    if (subscribeToErrorEvents) {
      lifecycleScope.launch {
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

  override fun onSaveInstanceState(outState: Bundle) {
    super.onSaveInstanceState(outState)
    outState.putBoolean(IS_ERROR_DIALOG_SHOWING, isErrorDialogShowing)
  }

  companion object {
    private const val IS_ERROR_DIALOG_SHOWING = "IS_ERROR_DIALOG_SHOWING"
  }
}
