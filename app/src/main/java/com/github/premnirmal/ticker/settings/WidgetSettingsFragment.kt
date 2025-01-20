package com.github.premnirmal.ticker.settings

import android.appwidget.AppWidgetManager
import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import android.view.View.OnClickListener
import android.view.ViewGroup
import android.widget.TextView
import androidx.annotation.ArrayRes
import androidx.appcompat.app.AlertDialog
import androidx.core.view.isVisible
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseFragment
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.createTimeString
import com.github.premnirmal.ticker.home.ChildFragment
import com.github.premnirmal.ticker.home.MainViewModel
import com.github.premnirmal.ticker.model.StocksProvider
import com.github.premnirmal.ticker.model.StocksProvider.FetchState
import com.github.premnirmal.ticker.showDialog
import com.github.premnirmal.ticker.ui.SettingsTextView
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.ticker.widget.WidgetData
import com.github.premnirmal.ticker.widget.WidgetDataProvider
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.FragmentWidgetSettingsBinding
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.threeten.bp.Instant
import org.threeten.bp.ZoneId
import org.threeten.bp.ZonedDateTime
import javax.inject.Inject

@AndroidEntryPoint
class WidgetSettingsFragment : BaseFragment<FragmentWidgetSettingsBinding>(), ChildFragment, OnClickListener {
	override val binding: (FragmentWidgetSettingsBinding) by viewBinding(FragmentWidgetSettingsBinding::inflate)
  companion object {
    private const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
    private const val ARG_SHOW_ADD_STOCKS = "show_add_stocks"
    private const val TRANSPARENT_BG = "transparent_bg"

    fun newInstance(widgetId: Int, showAddStocks: Boolean, transparentBg: Boolean = false): WidgetSettingsFragment {
      val fragment = WidgetSettingsFragment()
      val args = Bundle()
      args.putInt(ARG_WIDGET_ID, widgetId)
      args.putBoolean(ARG_SHOW_ADD_STOCKS, showAddStocks)
      args.putBoolean(TRANSPARENT_BG, transparentBg)
      fragment.arguments = args
      return fragment
    }
  }

  @Inject internal lateinit var widgetDataProvider: WidgetDataProvider
  @Inject internal lateinit var stocksProvider: StocksProvider
  private lateinit var adapter: WidgetPreviewAdapter
  private var showAddStocks = true
  private var transparentBg = true
  internal var widgetId = 0
  private val mainViewModel: MainViewModel by activityViewModels()
  override val simpleName: String = "WidgetSettingsFragment"

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
        widgetId = requireArguments().getInt(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    showAddStocks = requireArguments().getBoolean(ARG_SHOW_ADD_STOCKS, true)
    transparentBg = requireArguments().getBoolean(TRANSPARENT_BG, false)
  }

  override fun onViewCreated(
      view: View,
      savedInstanceState: Bundle?
  ) {
    super.onViewCreated(view, savedInstanceState)
    binding.settingAddStock.visibility = if (showAddStocks) View.VISIBLE else View.GONE
    if (!transparentBg) {
      binding.previewContainer.setBackgroundResource(R.drawable.bg_header)
    }
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
    adapter = WidgetPreviewAdapter(widgetData, widgetData.toImmutableData())
    binding.widgetLayout.list.adapter = adapter
    updatePreview(widgetData)

    arrayOf(
        binding.settingAddStock, binding.settingWidgetName, binding.settingLayoutType , binding.settingWidgetWidth,
        binding.settingBold, binding.settingAutosort, binding.settingHideHeader, binding.settingCurrency, binding.settingBackground, binding.settingTextColor
    ).forEach {
      it.setOnClickListener(this@WidgetSettingsFragment)
    }

    lifecycleScope.launch {
      stocksProvider.fetchState.collect {
        updatePreview(widgetData)
      }
    }
    lifecycleScope.launch {
      widgetData.autoSortEnabled.collect {
        binding.settingAutosortCheckbox.isChecked = it
      }
    }
  }

  override fun onClick(v: View) {
    val widgetData = widgetDataProvider.dataForWidgetId(widgetId)
    when (v.id) {
      R.id.setting_add_stock -> {
        mainViewModel.openSearch(widgetId)
      }
      R.id.setting_widget_name -> {
        v.setOnClickListener(null)
        (v as SettingsTextView).setIsEditable(true) { s ->
          widgetData.setWidgetName(s)
          setWidgetNameSetting(widgetData)
          v.setIsEditable(false)
          v.setOnClickListener(this)
          InAppMessage.showMessage(view as ViewGroup, R.string.widget_name_updated)
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
          InAppMessage.showMessage(view as ViewGroup, R.string.layout_updated_message)
        }
      }

      R.id.setting_background -> {
        showDialogPreference(R.array.backgrounds) { dialog, which ->
          widgetData.setBgPref(which)
          if (which == AppPreferences.SYSTEM) {
            widgetData.setTextColorPref(which)
            setTextColorSetting(widgetData)
          } else if (which == AppPreferences.TRANSLUCENT) {
            widgetData.setTextColorPref(AppPreferences.LIGHT)
            setTextColorSetting(widgetData)
          }
          setBgSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          InAppMessage.showMessage(view as ViewGroup, R.string.bg_updated_message)
        }
      }

      R.id.setting_text_color -> {
        showDialogPreference(R.array.text_colors) { dialog, which ->
          widgetData.setTextColorPref(which)
          setTextColorSetting(widgetData)
          dialog.dismiss()
          broadcastUpdateWidget()
          InAppMessage.showMessage(view as ViewGroup, R.string.text_color_updated_message)
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
          InAppMessage.showMessage(view as ViewGroup, R.string.widget_width_updated_message)
        }
      }

      R.id.setting_bold -> {
        val isChecked = !binding.settingBoldCheckbox.isChecked
        widgetData.setBoldEnabled(isChecked)
        setBoldSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_autosort -> {
        val isChecked = !binding.settingAutosortCheckbox.isChecked
        widgetData.setAutoSort(isChecked)
        setAutoSortSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_hide_header -> {
        val isChecked = !binding.settingHideHeaderCheckbox.isChecked
        widgetData.setHideHeader(isChecked)
        setHideHeaderSetting(widgetData)
        broadcastUpdateWidget()
      }
      R.id.setting_currency -> {
        val isChecked = !binding.settingCurrencyCheckbox.isChecked
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
    binding.settingWidgetName.setSubtitle(widgetData.widgetName())
  }

  private fun setWidgetSizeSetting(widgetData: WidgetData) {
    val widgetSizeTypeDesc = resources.getStringArray(R.array.widget_width_types)[widgetData.widgetSizePref()]
    binding.settingWidgetWidth.setSubtitle(widgetSizeTypeDesc)
  }
  private fun setLayoutTypeSetting(widgetData: WidgetData) {
    val layoutTypeDesc = resources.getStringArray(R.array.layout_types)[widgetData.layoutPref()]
    binding.settingLayoutType.setSubtitle(layoutTypeDesc)
  }

  private fun setBoldSetting(widgetData: WidgetData) {
    binding.settingBoldCheckbox.isChecked = widgetData.isBoldEnabled()
  }

  private fun setAutoSortSetting(widgetData: WidgetData) {
    binding.settingAutosortCheckbox.isChecked = widgetData.autoSortEnabled()
  }

  private fun setHideHeaderSetting(widgetData: WidgetData) {
    binding.settingHideHeaderCheckbox.isChecked = widgetData.hideHeader()
  }

  private fun setCurrencySetting(widgetData: WidgetData) {
    binding.settingCurrencyCheckbox.isChecked = widgetData.isCurrencyEnabled()
  }

  private fun setBgSetting(widgetData: WidgetData) {
    val bgDesc = resources.getStringArray(R.array.backgrounds)[widgetData.bgPref()]
    binding.settingBackground.setSubtitle(bgDesc)
  }

  private fun setTextColorSetting(widgetData: WidgetData) {
    val textColorDesc = resources.getStringArray(R.array.text_colors)[widgetData.textColorPref()]
    binding.settingTextColor.setSubtitle(textColorDesc)
  }

  private fun updatePreview(widgetData: WidgetData) {
    binding.widgetLayout.root.setBackgroundResource(widgetData.backgroundResource())
    val lastUpdatedText = when (val fetchState = stocksProvider.fetchState.value) {
      is FetchState.Success -> getString(R.string.last_fetch, fetchState.displayString)
      is FetchState.Failure -> getString(R.string.refresh_failed)
      else -> FetchState.NotFetched.displayString
    }
    binding.widgetLayout.root.findViewById<TextView>(R.id.last_updated).text = lastUpdatedText
    binding.widgetLayout.root.findViewById<View>(R.id.widget_header).isVisible = !widgetData.hideHeader()
    adapter.refresh(widgetData, widgetData.toImmutableData())
  }

  override fun scrollToTop() {
    binding.scrollView.smoothScrollTo(0, 0)
  }
}