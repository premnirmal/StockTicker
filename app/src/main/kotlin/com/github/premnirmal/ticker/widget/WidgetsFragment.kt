package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewGroup.MarginLayoutParams
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.BaseAdapter
import android.widget.TextView
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.getStatusBarHeight
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.settings.WidgetSettingsFragment
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_widgets.toolbar
import kotlinx.android.synthetic.main.fragment_widgets.widget_selection_spinner
import javax.inject.Inject

class WidgetsFragment : BaseFragment(), ChildFragment, OnItemSelectedListener {

  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  private lateinit var widgetDataList: List<WidgetData>
  private var currentWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
  override val simpleName: String = "WidgetsFragment"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_widgets, container, false)
  }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    (toolbar.layoutParams as MarginLayoutParams).topMargin = requireContext().getStatusBarHeight()
    widgetDataList = widgetDataProvider.getAppWidgetIds()
        .map {
          widgetDataProvider.dataForWidgetId(it)
        }
        .sortedBy { it.widgetName() }
    widget_selection_spinner.adapter = WidgetSpinnerAdapter(widgetDataList)
    widget_selection_spinner.onItemSelectedListener = this

    arguments?.let {
      selectWidgetFromBundle(it)
    }

    savedInstanceState?.let {
      selectWidgetFromBundle(it)
    }
  }

  private fun selectWidgetFromBundle(bundle: Bundle) {
    val widgetId = bundle.getInt(ARG_WIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val position = widgetDataList.indexOfFirst { it.widgetId == widgetId }
      widget_selection_spinner.setSelection(position)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putInt(ARG_WIDGET_ID, currentWidgetId)
    super.onSaveInstanceState(outState)
  }

  private fun setWidgetFragment(widgetId: Int) {
    val fragment = WidgetSettingsFragment.newInstance(widgetId, false)
    childFragmentManager.beginTransaction()
        .replace(R.id.child_fragment_container, fragment)
        .commit()
    currentWidgetId = widgetId
  }

  // ChildFragment

  override fun setData(bundle: Bundle) {
    if (isVisible) {
      val widgetId = bundle.getInt(ARG_WIDGET_ID)
      setWidgetFragment(widgetId)
    } else {
      arguments = bundle
    }
  }

  // OnItemSelectedListener

  override fun onNothingSelected(parent: AdapterView<*>?) {

  }

  override fun onItemSelected(
    parent: AdapterView<*>?,
    view: View?,
    position: Int,
    id: Long
  ) {
    setWidgetFragment(widgetDataList[position].widgetId)
  }

  class WidgetSpinnerAdapter(private val data: List<WidgetData>) : BaseAdapter() {

    override fun getView(
      position: Int,
      convertView: View?,
      parent: ViewGroup
    ): View {
      val widgetData = getItem(position)
      val view =
        convertView ?: LayoutInflater.from(parent.context).inflate(
            R.layout.item_widget, parent,
            false
        )
      val nameTextView = view.findViewById<TextView>(R.id.widget_name_text)
      nameTextView.text = widgetData.widgetName()
      return view
    }

    override fun getItem(position: Int): WidgetData = data[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getCount(): Int = data.size
  }
}