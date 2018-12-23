package com.github.premnirmal.ticker.ui

import androidx.recyclerview.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.ui.WidgetListAdapter.WidgetVH
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

class WidgetListAdapter(private val listener: WidgetClickListener) :
    androidx.recyclerview.widget.RecyclerView.Adapter<WidgetVH>() {

  interface WidgetClickListener {
    fun onWidgetClick(widgetId: Int)
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider

  init {
    Injector.appComponent.inject(this)
  }

  override fun getItemCount(): Int = widgetDataProvider.getAppWidgetIds().size

  override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): WidgetVH {
    val view = LayoutInflater.from(parent.context).inflate(R.layout.item_widget, parent, false)
    return WidgetVH(view)
  }

  override fun onBindViewHolder(holder: WidgetVH, position: Int) {
    val widgetId = widgetDataProvider.getAppWidgetIds()[position]
    val data = widgetDataProvider.dataForWidgetId(widgetId)
    holder.update(data, listener)
  }

  class WidgetVH(itemView: View) : androidx.recyclerview.widget.RecyclerView.ViewHolder(itemView) {

    fun update(data: WidgetData, listener: WidgetClickListener) {

    }
  }
}