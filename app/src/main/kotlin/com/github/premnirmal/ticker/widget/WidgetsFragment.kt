package com.github.premnirmal.ticker.widget

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import androidx.recyclerview.widget.LinearLayoutManager
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.settings.WidgetSettingsActivity
import com.github.premnirmal.ticker.ui.WidgetListAdapter
import com.github.premnirmal.ticker.ui.WidgetListAdapter.WidgetClickListener
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_widgets.recycler_view
import kotlinx.android.synthetic.main.fragment_widgets.toolbar

class WidgetsFragment : BaseFragment(), WidgetClickListener {

  private lateinit var adapter: WidgetListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_widgets, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = context!!.getStatusBarHeight()
    adapter = WidgetListAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(androidx.recyclerview.widget.DividerItemDecoration(activity,
        androidx.recyclerview.widget.DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
  }

  override fun onResume() {
    super.onResume()
    adapter.notifyDataSetChanged()
  }

  override fun onWidgetClick(widgetId: Int) {
    startActivity(WidgetSettingsActivity.launchIntent(context!!, widgetId))
  }
}