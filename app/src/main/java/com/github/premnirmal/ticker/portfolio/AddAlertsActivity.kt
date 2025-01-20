package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.viewModels
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.viewBinding
import com.github.premnirmal.tickerwidget.R
import com.github.premnirmal.tickerwidget.databinding.ActivityAlertsBinding
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import javax.inject.Inject

@AndroidEntryPoint
class AddAlertsActivity : BaseActivity<ActivityAlertsBinding>() {
	override val binding: (ActivityAlertsBinding) by viewBinding(ActivityAlertsBinding::inflate)

  companion object {
    const val TICKER = "TICKER"
  }

  internal lateinit var ticker: String
  private val viewModel: AlertsViewModel by viewModels()
  override val simpleName: String = "AddAlertsActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    binding.toolbar.setNavigationOnClickListener {
      finish()
    }

    binding.alertsDisabledMessage.visibility = if (appPreferences.notificationAlerts()) View.GONE else View.VISIBLE
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)!!
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    viewModel.symbol = ticker
    binding.tickerName.text = ticker

    val quote = viewModel.quote
    val alertAbove = quote?.getAlertAbove() ?: 0f
    if (alertAbove != 0.0f) {
      binding.alertAboveInputEditText.setText(appPreferences.selectedDecimalFormat.format(alertAbove))
    } else {
      binding.alertAboveInputEditText.setText("")
    }
    val alertBelow = quote?.getAlertBelow() ?: 0f
    if (alertBelow != 0.0f) {
      binding.alertBelowInputEditText.setText(appPreferences.selectedDecimalFormat.format(alertBelow))
    } else {
      binding.alertBelowInputEditText.setText("")
    }

    binding.addButton.setOnClickListener { onAddClicked() }
  }

  private fun onAddClicked() {
    val alertAboveText = binding.alertAboveInputEditText.text.toString()
    val alertBelowText = binding.alertBelowInputEditText.text.toString()
    var alertAbove = 0f
    var alertBelow = 0f
    var success = true

    if (alertAboveText.isNotEmpty()) {
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        alertAbove = numberFormat.parse(alertAboveText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        binding.alertAboveInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
    }

    if (alertBelowText.isNotEmpty()) {
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        alertBelow = numberFormat.parse(alertBelowText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        binding.alertBelowInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
    }

    if (alertAbove > 0.0f && alertBelow > 0.0f) {
      if (binding.alertAboveInputEditText.isFocused) {
        if (success && alertBelow >= alertAbove) {
          binding.alertAboveInputLayout.error = getString(R.string.alert_below_error)
          success = false
        }
      } else {
        if (success && alertBelow >= alertAbove) {
          binding.alertBelowInputLayout.error = getString(R.string.alert_above_error)
          success = false
        }
      }
    }
    if (success) {
      viewModel.setAlerts(alertAbove, alertBelow)
      updateActivityResult()
      dismissKeyboard()
      finish()
    }
  }

  private fun updateActivityResult() {
    val data = Intent()
    setResult(Activity.RESULT_OK, data)
  }
}