package com.github.premnirmal.ticker.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Build.VERSION
import android.os.Build.VERSION_CODES
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_widget_settings.toolbar

class WidgetSettingsActivity : BaseActivity(), WidgetSettingsFragment.Parent {

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  internal var widgetId = 0
  override val simpleName: String = "WidgetSettingsActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_widget_settings)
    toolbar.setNavigationOnClickListener {
      setOkResult()
      finish()
    }
    if (VERSION.SDK_INT >= VERSION_CODES.LOLLIPOP) {
      toolbar.navigationIcon?.setTint(resources.getColor(R.color.icon_tint))
      toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
    }
    toolbar.inflateMenu(R.menu.menu_widget_settings)
    toolbar.setOnMenuItemClickListener {
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
          .add(R.id.fragment_container, WidgetSettingsFragment.newInstance(widgetId, true))
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