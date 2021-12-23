package com.github.premnirmal.ticker.base

import android.content.Context
import android.os.Bundle
import android.os.PersistableBundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.R
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  abstract val simpleName: String
  @Inject internal lateinit var stocksProvider: IStocksProvider
  @Inject internal lateinit var analytics: Analytics
  open val subscribeToErrorEvents = true
  private var isErrorDialogShowing = false

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
  }

  override fun onCreate(
      savedInstanceState: Bundle?,
      persistentState: PersistableBundle?
  ) {
    super.onCreate(savedInstanceState, persistentState)
    analytics.trackScreenView(simpleName, this)
    savedInstanceState?.let { isErrorDialogShowing = it.getBoolean(IS_ERROR_DIALOG_SHOWING, false) }
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

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    try {
      super.onRestoreInstanceState(savedInstanceState)
    } catch (ex: Throwable) {
      // android bug
      Timber.w(ex)
    }
  }

  override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  protected fun showErrorAndFinish() {
    InAppMessage.showToast(this, R.string.error_symbol)
    finish()
  }

  companion object {
    private const val IS_ERROR_DIALOG_SHOWING = "IS_ERROR_DIALOG_SHOWING"
  }
}
