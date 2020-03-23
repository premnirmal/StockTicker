package com.github.premnirmal.ticker.portfolio.search

import android.appwidget.AppWidgetManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.tickerwidget.R

class SearchActivity : BaseActivity() {
  override val simpleName: String = "SearchActivity"

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID

    fun launchIntent(
      context: Context,
      widgetId: Int
    ): Intent {
      val intent = Intent(context, SearchActivity::class.java)
      intent.putExtra(ARG_WIDGET_ID, widgetId)
      return intent
    }
  }

  var widgetId: Int = -1

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_search)
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, SearchFragment.newInstance(widgetId, showNavIcon = true))
          .commit()
    }
  }
}