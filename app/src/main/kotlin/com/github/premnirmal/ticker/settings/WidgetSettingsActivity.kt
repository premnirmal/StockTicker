package com.github.premnirmal.ticker.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.tickerwidget.R

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
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val result = Intent()
      result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
      setResult(Activity.RESULT_OK, result)
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, WidgetSettingsFragment.newInstance(widgetId))
          .commit()
    }
  }

  override fun openSearch(widgetId: Int) {
    val intent = SearchActivity.launchIntent(this, widgetId)
    startActivity(intent)
  }
}