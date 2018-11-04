package com.github.premnirmal.ticker.widget

import android.os.Bundle
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.ui.WidgetListAdapter
import com.github.premnirmal.ticker.ui.WidgetListAdapter.WidgetClickListener
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_widgets.recycler_view

class WidgetsFragment: BaseFragment(), WidgetClickListener {

  private lateinit var adapter: WidgetListAdapter

  override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
    savedInstanceState: Bundle?): View? {
    return inflater.inflate(R.layout.fragment_widgets, container, false)
  }

  override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
    super.onViewCreated(view, savedInstanceState)
    adapter = WidgetListAdapter(this)
    recycler_view.layoutManager = LinearLayoutManager(activity)
    recycler_view.addItemDecoration(DividerItemDecoration(activity, DividerItemDecoration.VERTICAL))
    recycler_view.adapter = adapter
  }

  override fun onWidgetClick(widgetId: Int) {

  }
}