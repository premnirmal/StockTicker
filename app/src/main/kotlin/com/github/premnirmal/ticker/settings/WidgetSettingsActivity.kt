package com.github.premnirmal.ticker.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import android.view.LayoutInflater
import androidx.core.content.ContextCompat
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityWidgetSettingsBinding

class WidgetSettingsActivity : BaseActivity<ActivityWidgetSettingsBinding>(), WidgetSettingsFragment.Parent {

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  internal var widgetId = 0
  override val simpleName: String = "WidgetSettingsActivity"


  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    binding.toolbar.setNavigationOnClickListener {
      setOkResult()
      finish()
    }
    binding.toolbar.navigationIcon?.setTint(ContextCompat.getColor(this, R.color.icon_tint))
    binding.toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
    binding.toolbar.inflateMenu(R.menu.menu_widget_settings)
    binding.toolbar.setOnMenuItemClickListener {
      setOkResult()
      finish()
      return@setOnMenuItemClickListener true
    }
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      setOkResult()
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, WidgetSettingsFragment.newInstance(widgetId,
              showAddStocks = true, transparentBg = true
          ))
          .commit()
    }
  }

  private fun setOkResult() {
    val result = Intent()
    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    setResult(Activity.RESULT_OK, result)
  }

  override fun openSearch(widgetId: Int) {
    val intent = SearchActivity.launchIntent(this, widgetId)
    startActivity(intent)
  }
}