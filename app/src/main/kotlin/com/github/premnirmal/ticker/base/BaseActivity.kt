package com.github.premnirmal.ticker.base

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.PersistableBundle
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.AsyncBus
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.events.ErrorEvent
import com.github.premnirmal.ticker.events.UnauthorizedEvent
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.tickerwidget.BuildConfig
import com.github.premnirmal.tickerwidget.R
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  abstract val simpleName: String
  @Inject internal lateinit var bus: AsyncBus
  @Inject internal lateinit var analytics: Analytics

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase))
  }

  override fun onCreate(
      savedInstanceState: Bundle?,
      persistentState: PersistableBundle?
  ) {
    super.onCreate(savedInstanceState, persistentState)
    analytics.trackScreenView(simpleName, this)
  }

  override fun onResume() {
    super.onResume()
    lifecycleScope.launch {
      val errorFlow = bus.receive<ErrorEvent>()
      errorFlow.collect { event ->
        showDialog(event.message)
      }
    }
    lifecycleScope.launch {
      val unauthorizedFlow = bus.receive<UnauthorizedEvent>()
      unauthorizedFlow.collect {
        showIllegalErrorAndFinish()
      }
    }
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

  protected fun dismissKeyboard() {
    val view = currentFocus
    if (view is TextView) {
      val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
      imm.hideSoftInputFromWindow(view.windowToken, 0)
    }
  }

  protected fun showIllegalErrorAndFinish() {
    if (!BuildConfig.DEBUG) {
      showDialog(getString(R.string.error_illegal_app),
          DialogInterface.OnClickListener { _, _ ->
            finish()
          },
          cancelable = false)
    }
  }
}
