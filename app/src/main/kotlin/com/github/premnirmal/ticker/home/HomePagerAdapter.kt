package com.github.premnirmal.ticker.home

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.support.v4.app.Fragment
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.StockWidget

internal class HomePagerAdapter(fm: FragmentManager,
    val packageName: String,
    val widgetManager: AppWidgetManager)
  : FragmentPagerAdapter(fm) {


  override fun getItem(position: Int): Fragment {
    val appWidgetIds = appWidgetIds
    if (appWidgetIds.isEmpty()) {
      return PortfolioFragment.newInstance()
    } else {
      return PortfolioFragment.newInstance(appWidgetIds[position])
    }
  }

  private val appWidgetIds: IntArray
    get() {
      val appWidgetIds = widgetManager.getAppWidgetIds(
          ComponentName(packageName, StockWidget::class.java.name))
      return appWidgetIds
    }

  override fun getCount(): Int {
    val appWidgetIds = appWidgetIds
    if (appWidgetIds.isEmpty()) return 1 else return appWidgetIds.size
  }

  override fun getPageTitle(position: Int): CharSequence {
    if (count == 1) {
      return ""
    } else {
      val id = position + 1
      return "Widget #$id"
    }
  }
}