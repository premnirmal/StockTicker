package com.github.premnirmal.ticker.home

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.github.premnirmal.ticker.portfolio.PortfolioFragment
import com.github.premnirmal.ticker.widget.WidgetData

class HomePagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

  private var widgetDataList = emptyList<WidgetData>()

  override fun getItem(position: Int): Fragment {
    return if (widgetDataList.isEmpty()) {
      PortfolioFragment.newInstance()
    } else {
      PortfolioFragment.newInstance(widgetDataList[position].widgetId)
    }
  }

  fun setData(widgetData: List<WidgetData>) {
    this.widgetDataList = widgetData
    notifyDataSetChanged()
  }

  override fun getCount(): Int {
    return widgetDataList.size
  }

  override fun getPageTitle(position: Int): CharSequence {
    val widgetData = widgetDataList[position]
    return widgetData.widgetName()
  }

  override fun getPageWidth(position: Int): Float =
    if (count > 1) 0.95f else super.getPageWidth(position)
}