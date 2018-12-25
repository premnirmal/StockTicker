package com.github.premnirmal.ticker.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.ui.WidgetListAdapter.WidgetVH
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import javax.inject.Inject

class WidgetListAdapter(private val listener: WidgetClickListener) :
    RecyclerView.Adapter<WidgetVH>() {

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

  class WidgetVH(itemView: View) : RecyclerView.ViewHolder(itemView) {
    private val textView = itemView.findViewById<TextView>(R.id.widget_name_text)

    fun update(data: WidgetData, listener: WidgetClickListener) {
      textView.text = data.widgetName()
      textView.setOnClickListener { listener.onWidgetClick(data.widgetId) }
    }
  }
}