package com.github.premnirmal.ticker.widget

import android.appwidget.AppWidgetManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.AdapterView.OnItemSelectedListener
import android.widget.BaseAdapter
import android.widget.TextView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.settings.WidgetSettingsFragment
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentWidgetsBinding
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class WidgetsFragment : BaseFragment<FragmentWidgetsBinding>(), ChildFragment, OnItemSelectedListener {
	override val binding: (FragmentWidgetsBinding) by viewBinding(FragmentWidgetsBinding::inflate)
  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  private lateinit var widgetDataList: List<WidgetData>
  private var currentWidgetId = AppWidgetManager.INVALID_APPWIDGET_ID
  override val simpleName: String = "WidgetsFragment"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
      }

  override fun onViewCreated(
    view: View,
    savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    // Inset toolbar from window top inset
    ViewCompat.setOnApplyWindowInsetsListener(view) { _, insets ->
      binding.toolbar.updateLayoutParams<ViewGroup.MarginLayoutParams> {
        this.topMargin = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top
      }
      insets
    }
    widgetDataList = widgetDataProvider.getAppWidgetIds()
        .map {
          widgetDataProvider.dataForWidgetId(it)
        }
        .sortedBy { it.widgetName() }
    binding.widgetSelectionSpinner.adapter = WidgetSpinnerAdapter(widgetDataList)
    binding.widgetSelectionSpinner.onItemSelectedListener = this

    var selected = false

    arguments?.let {
      selected = true
      selectWidgetFromBundle(it)
    }

    savedInstanceState?.let {
      selected = true
      selectWidgetFromBundle(it)
    }
    if (!selected && widgetDataList.isNotEmpty()) {
      setWidgetFragment(widgetDataList[0].widgetId)
    }
  }

  private fun selectWidgetFromBundle(bundle: Bundle) {
    val widgetId = bundle.getInt(ARG_WIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      val position = widgetDataList.indexOfFirst { it.widgetId == widgetId }
      binding.widgetSelectionSpinner.setSelection(position)
    }
  }

  override fun onSaveInstanceState(outState: Bundle) {
    outState.putInt(ARG_WIDGET_ID, currentWidgetId)
    super.onSaveInstanceState(outState)
  }

  private fun setWidgetFragment(widgetId: Int) {
    val fragment = WidgetSettingsFragment.newInstance(widgetId, false, transparentBg = false)
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

  override fun scrollToTop() {
    (childFragmentManager.findFragmentById(R.id.child_fragment_container) as? ChildFragment)?.scrollToTop()
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