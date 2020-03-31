package com.github.premnirmal.ticker.home

import android.appwidget.AppWidgetManager
import androidx.fragment.app.FragmentPagerAdapter
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import javax.inject.Inject

class HomePagerAdapter(fm: androidx.fragment.app.FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  init {
    Injector.appComponent.inject(this)
  }

  override fun getItem(position: Int): androidx.fragment.app.Fragment {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty()) {
      PortfolioFragment.newInstance()
    } else {
      PortfolioFragment.newInstance(appWidgetIds[position])
    }
  }

  private fun appWidgetIds(): IntArray = widgetDataProvider.getAppWidgetIds()

  override fun getCount(): Int {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty()) 1 else appWidgetIds.size
  }

  override fun getPageTitle(position: Int): CharSequence {
    val appWidgetIds = appWidgetIds()
    return if (appWidgetIds.isEmpty() || appWidgetIds[position] == AppWidgetManager.INVALID_APPWIDGET_ID) {
      ""
    } else {
      val widgetData = widgetDataProvider.dataForWidgetId(appWidgetIds[position])
      widgetData.widgetName()
    }
  }

  override fun getPageWidth(position: Int): Float =
    if (count > 1) 0.95f else super.getPageWidth(position)
}