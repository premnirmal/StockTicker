package com.github.premnirmal.ticker.home

import android.appwidget.AppWidgetManager
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import javax.inject.Inject

class HomePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {

  @Inject lateinit internal var widgetDataProvider: WidgetDataProvider

  init {
    Injector.appComponent.inject(this)
  }

  override fun getItem(position: Int): Fragment {
    val appWidgetIds = appWidgetIds()
    if (appWidgetIds.isEmpty()) {
      return PortfolioFragment.newInstance()
    } else {
      return PortfolioFragment.newInstance(appWidgetIds[position])
    }
  }

  private fun appWidgetIds(): IntArray {
    val appWidgetIds = widgetDataProvider.getAppWidgetIds()
    return appWidgetIds
  }

  override fun getCount(): Int {
    val appWidgetIds = appWidgetIds()
    if (appWidgetIds.isEmpty()) return 1 else return appWidgetIds.size
  }

  override fun getPageTitle(position: Int): CharSequence {
    val appWidgetIds = appWidgetIds()
    if (appWidgetIds.isEmpty() ||
        appWidgetIds[position] == AppWidgetManager.INVALID_APPWIDGET_ID) {
      return ""
    } else {
      val widgetData = widgetDataProvider.dataForWidgetId(appWidgetIds[position])
      if (widgetData.widgetName().isNotBlank()) {
        return widgetData.widgetName()
      } else {
        val index = position + 1
        return "Widget #$index"
      }
    }
  }
}