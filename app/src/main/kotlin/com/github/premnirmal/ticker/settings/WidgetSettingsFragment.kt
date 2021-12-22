package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.model.IStocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.SettingsTextView
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.fragment_widget_settings.*
import kotlinx.android.synthetic.main.widget_2x1.list
import kotlinx.android.synthetic.main.widget_header.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

class WidgetSettingsFragment : BaseFragment(), ChildFragment, OnClickListener {

  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    private const val ARG_SHOW_ADD_STOCKS = "show_add_stocks"

    fun newInstance(widgetId: Int, showAddStocks: Boolean): WidgetSettingsFragment {
      val fragment = WidgetSettingsFragment()
      val args = Bundle()
      args.putInt(ARG_WIDGET_ID, widgetId)
      args.putBoolean(ARG_SHOW_ADD_STOCKS, showAddStocks)
      fragment.arguments = args
      return fragment
    }
  }

  interface Parent {
    fun openSearch(widgetId: Int)
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: IStocksProvider
  private lateinit var adapter: WidgetPreviewAdapter
  private var showAddStocks = true
  private val parent: Parent
    get() = activity as Parent
  internal var widgetId = 0
  override val simpleName: String = "WidgetSettingsFragment"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.appComponent.inject(this)
    widgetId = requireArguments().getInt(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    showAddStocks = requireArguments().getBoolean(ARG_SHOW_ADD_STOCKS, true)
  }

  override fun onCreateView(
      inflater: LayoutInflater,
      container: ViewGroup?,
      savedInstanceState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.fragment_widget_settings, container, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    setting_add_stock.visibility = if (showAddStocks) View.VISIBLE else View.GONE
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    setWidgetNameSetting(widgetData)
    setLayoutTypeSetting(widgetData)
    setWidgetSizeSetting(widgetData)
    setBoldSetting(widgetData)
    setAutoSortSetting(widgetData)
    setHideHeaderSetting(widgetData)
    setCurrencySetting(widgetData)
    setBgSetting(widgetData)
    setTextColorSetting(widgetData)
    adapter = WidgetPreviewAdapter(widgetData)
    list.adapter = adapter
    updatePreview(widgetData)

    arrayOf(
        setting_add_stock, setting_widget_name, setting_layout_type , setting_widget_width,
        setting_bold, setting_autosort, setting_hide_header, setting_currency, setting_background, setting_text_color
    ).forEach {
      it.setOnClickListener(this@WidgetSettingsFragment)
    }

    lifecycleScope.launch {
      stocksProvider.fetchState.collect {
        updatePreview(widgetData)
      }
    }
  }

  override fun onClick(v: View) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    when (v.id) {
      R.id.setting_add_stock -> {
        parent.openSearch(widgetId)
      }
      R.id.setting_widget_name -> {
        v.setOnClickListener(null)
        (v as SettingsTextView).setIsEditable(true) { s ->
          widgetData.setWidgetName(s)
          setWidgetNameSetting(widgetData)
          v.setIsEditable(false)
          v.setOnClickListener(this)
          InAppMessage.showMessage(requireActivity(), R.string.widget_name_updated)
        }
      }
      R.id.setting_layout_type -> {
        showDialogPreference(R.array.layout_types) { dialog, which ->
          widgetData.setLayoutPref(which)
          setLayoutTypeSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          if (which == 2) {
            showDialog(getString(R.string.change_instructions))
          }
          InAppMessage.showMessage(requireActivity(), R.string.layout_updated_message)
        }
      }

      R.id.setting_background -> {
        showDialogPreference(R.array.backgrounds) { dialog, which ->
          widgetData.setBgPref(which)
          if (which == AppPreferences.SYSTEM) {
            widgetData.setTextColorPref(which)
            setTextColorSetting(widgetData)
          }
          setBgSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          InAppMessage.showMessage(requireActivity(), R.string.bg_updated_message)
        }
      }

      R.id.setting_text_color -> {
        showDialogPreference(R.array.text_colors) { dialog, which ->
          widgetData.setTextColorPref(which)
          setTextColorSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          InAppMessage.showMessage(requireActivity(), R.string.text_color_updated_message)
        }
      }

      R.id.setting_widget_width -> {
        showDialogPreference(
            R.array.widget_width_types
        ) { dialog, which ->
          widgetData.setWidgetSizePref(which)
          setWidgetSizeSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          InAppMessage.showMessage(requireActivity(), R.string.widget_width_updated_message)
        }
      }

      R.id.setting_bold -> {
        val isChecked = !setting_bold_checkbox.isChecked
        widgetData.setBoldEnabled(isChecked)
        setBoldSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_autosort -> {
        val isChecked = !setting_autosort_checkbox.isChecked
        widgetData.setAutoSort(isChecked)
        setAutoSortSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_hide_header -> {
        val isChecked = !setting_hide_header_checkbox.isChecked
        widgetData.setHideHeader(isChecked)
        setHideHeaderSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_currency -> {
        val isChecked = !setting_currency_checkbox.isChecked
        widgetData.setCurrencyEnabled(isChecked)
        setCurrencySetting(widgetData)
        broadcastUpdateWidget()
      }
    }
  }

  private fun broadcastUpdateWidget() {
    widgetDataProvider.broadcastUpdateWidget(widgetId)
    updatePreview(widgetDataProvider.dataForWidgetId(widgetId))
  }

  private fun showDialogPreference(
      @ArrayRes itemRes: Int,
      listener: DialogInterface.OnClickListener
  ) {
    AlertDialog.Builder(requireContext())
        .setItems(itemRes, listener)
        .create()
        .show()
  }

  private fun setWidgetNameSetting(widgetData: WidgetData) {
    setting_widget_name.setSubtitle(widgetData.widgetName())
  }

  private fun setWidgetSizeSetting(widgetData: WidgetData) {
    val widgetSizeTypeDesc = resources.getStringArray(R.array.widget_width_types)[widgetData.widgetSizePref()]
    setting_widget_width.setSubtitle(widgetSizeTypeDesc)
  }
  private fun setLayoutTypeSetting(widgetData: WidgetData) {
    val layoutTypeDesc = resources.getStringArray(R.array.layout_types)[widgetData.layoutPref()]
    setting_layout_type.setSubtitle(layoutTypeDesc)
  }

  private fun setBoldSetting(widgetData: WidgetData) {
    setting_bold_checkbox.isChecked = widgetData.isBoldEnabled()
  }

  private fun setAutoSortSetting(widgetData: WidgetData) {
    setting_autosort_checkbox.isChecked = widgetData.autoSortEnabled()
  }

  private fun setHideHeaderSetting(widgetData: WidgetData) {
    setting_hide_header_checkbox.isChecked = widgetData.hideHeader()
  }

  private fun setCurrencySetting(widgetData: WidgetData) {
    setting_currency_checkbox.isChecked = widgetData.isCurrencyEnabled()
  }

  private fun setBgSetting(widgetData: WidgetData) {
    val bgDesc = resources.getStringArray(R.array.backgrounds)[widgetData.bgPref()]
    setting_background.setSubtitle(bgDesc)
  }

  private fun setTextColorSetting(widgetData: WidgetData) {
    val textColorDesc = resources.getStringArray(R.array.text_colors)[widgetData.textColorPref()]
    setting_text_color.setSubtitle(textColorDesc)
  }

  private fun updatePreview(widgetData: WidgetData) {
    widget_layout.setBackgroundResource(widgetData.backgroundResource())
    val lastUpdatedText = when (val fetchState = stocksProvider.fetchState.value) {
      is FetchState.Success -> getString(R.string.last_fetch, fetchState.displayString)
      is FetchState.Failure -> getString(R.string.refresh_failed)
      else -> FetchState.NotFetched.displayString
    }
    last_updated.text = lastUpdatedText
    val nextUpdateMs = stocksProvider.nextFetchMs.value
    val instant = Instant.ofEpochMilli(nextUpdateMs)
    val time = ZonedDateTime.ofInstant(instant, ZoneId.systemDefault())
    val nextUpdate = time.createTimeString()
    val nextUpdateText: String = getString(R.string.next_fetch, nextUpdate)
    next_update.text = nextUpdateText
    widget_header.isVisible = !widgetData.hideHeader()
    adapter.refresh(widgetData)
  }

  override fun scrollToTop() {
    scroll_view.smoothScrollTo(0, 0)
  }
}