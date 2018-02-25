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

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  init {
    Injector.appComponent.inject(this)
  }

  override fun getItem(position: Int): Fragment {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty()) {
      PortfolioFragment.newInstance()
    } else {
      PortfolioFragment.newInstance(appWidgetIds[position])
    }
  }

  private fun appWidgetIds(): IntArray {
    val appWidgetIds = widgetDataProvider.getAppWidgetIds()
    return appWidgetIds
  }

  override fun getCount(): Int {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty()) 1 else appWidgetIds.size
  }

  override fun getPageTitle(position: Int): CharSequence {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty() ||
        appWidgetIds[position] == AppWidgetManager.INVALID_APPWIDGET_ID) {
      ""
    } else {
      val widgetData = widgetDataProvider.dataForWidgetId(appWidgetIds[position])
      if (widgetData.widgetName().isNotBlank()) {
        widgetData.widgetName()
      } else {
        val index = position + 1
        "Widget #$index"
      }
    }
  }

  override fun getPageWidth(position: Int): Float =
      if (count > 1) 0.95f else super.getPageWidth(position)
}