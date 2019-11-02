package com.github.premnirmal.ticker.base

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.PersistableBundle
import android.view.inputmethod.InputMethodManager
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import com.github.premnirmal.ticker.analytics.Analytics
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.RxBus
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.tickerwidget.R
import io.github.inflationx.viewpump.ViewPumpContextWrapper
import javax.inject.Inject

/**
 * Created by premnirmal on 2/26/16.
 */
abstract class BaseActivity : AppCompatActivity() {

  abstract val simpleName: String
  @Inject internal lateinit var bus: RxBus
  @Inject internal lateinit var analytics: Analytics

  override fun attachBaseContext(newBase: Context) {
    super.attachBaseContext(ViewPumpContextWrapper.wrap(newBase));
  }

  override fun onCreate(
    savedInstanceState: Bundle?,
    persistentState: PersistableBundle?
  ) {
    super.onCreate(savedInstanceState, persistentState)
    analytics.trackScreenView(simpleName)
  }

  override fun onOptionsItemSelected(item: android.view.MenuItem): Boolean {
    if (item.itemId == android.R.id.home) {
      onBackPressed()
      return true
    }
    return super.onOptionsItemSelected(item)
  }

  fun updateToolbar(toolbar: Toolbar) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      toolbar.setPadding(
          toolbar.paddingLeft, getStatusBarHeight(), toolbar.paddingRight,
          toolbar.paddingBottom
      )
    }
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
}
