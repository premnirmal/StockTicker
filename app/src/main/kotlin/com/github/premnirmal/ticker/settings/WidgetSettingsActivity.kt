package com.github.premnirmal.ticker.settings

import android.app.Activity
import android.appwidget.AppWidgetManager
import android.content.Intent
import android.graphics.PorterDuff
import android.os.Bundle
import androidx.activity.viewModels
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.home.MainViewModel
import com.github.premnirmal.ticker.portfolio.search.SearchActivity
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityWidgetSettingsBinding
import com.google.android.material.color.MaterialColors
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class WidgetSettingsActivity : BaseActivity<ActivityWidgetSettingsBinding>() {
	override val binding: (ActivityWidgetSettingsBinding) by viewBinding(ActivityWidgetSettingsBinding::inflate)

  companion object {
    const val ARG_WIDGET_ID = AppWidgetManager.EXTRA_APPWIDGET_ID
  }

  internal var widgetId = 0
  override val simpleName: String = "WidgetSettingsActivity"
  private val viewModel: MainViewModel by viewModels()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    binding.toolbar.setNavigationOnClickListener {
      setOkResult()
      finish()
    }
    val tint = MaterialColors.getColor(binding.toolbar, com.google.android.material.R.attr.colorOnSurfaceVariant)
    binding.toolbar.navigationIcon?.setTint(tint)
    binding.toolbar.navigationIcon?.setTintMode(PorterDuff.Mode.SRC_IN)
    binding.toolbar.inflateMenu(R.menu.menu_widget_settings)
    binding.toolbar.setOnMenuItemClickListener {
      setOkResult()
      finish()
      return@setOnMenuItemClickListener true
    }
    widgetId = intent.getIntExtra(ARG_WIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
    if (widgetId != AppWidgetManager.INVALID_APPWIDGET_ID) {
      setOkResult()
    } else {
      setResult(Activity.RESULT_CANCELED)
    }
    if (savedInstanceState == null) {
      supportFragmentManager.beginTransaction()
          .add(R.id.fragment_container, WidgetSettingsFragment.newInstance(widgetId,
              showAddStocks = true, transparentBg = true
          ))
          .commit()
    }
    viewModel.openSearchWidgetId.observe(this) {
      it?.let {
        openSearch(it)
        viewModel.resetOpenSearch()
      }
    }
  }

  private fun setOkResult() {
    val result = Intent()
    result.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, widgetId)
    setResult(Activity.RESULT_OK, result)
  }

  private fun openSearch(widgetId: Int) {
    val intent = SearchActivity.launchIntent(this, widgetId)
    startActivity(intent)
  }
}