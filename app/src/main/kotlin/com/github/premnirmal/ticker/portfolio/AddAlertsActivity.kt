package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_alerts.addButton
import kotlinx.android.synthetic.main.activity_alerts.alertAboveInputEditText
import kotlinx.android.synthetic.main.activity_alerts.alertAboveInputLayout
import kotlinx.android.synthetic.main.activity_alerts.alertBelowInputEditText
import kotlinx.android.synthetic.main.activity_alerts.alertBelowInputLayout
import kotlinx.android.synthetic.main.activity_alerts.alerts_disabled_message
import kotlinx.android.synthetic.main.activity_alerts.tickerName
import kotlinx.android.synthetic.main.activity_alerts.toolbar
import java.text.NumberFormat
import javax.inject.Inject

class AddAlertsActivity : BaseActivity() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  internal lateinit var ticker: String
  private lateinit var viewModel: AlertsViewModel
  override val simpleName: String = "AddAlertsActivity"
  @Inject internal lateinit var appPreferences: AppPreferences

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_alerts)
    toolbar.setNavigationOnClickListener {
      finish()
    }

    alerts_disabled_message.visibility = if (appPreferences.notificationAlerts()) View.GONE else View.VISIBLE
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)!!
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    viewModel = ViewModelProvider(this).get(AlertsViewModel::class.java)
    viewModel.symbol = ticker
    tickerName.text = ticker

    val quote = viewModel.quote
    val alertAbove = quote?.getAlertAbove() ?: 0f
    if (alertAbove != 0.0f) {
      alertAboveInputEditText.setText(Quote.selectedFormat.format(alertAbove))
    } else {
      alertAboveInputEditText.setText("")
    }
    val alertBelow = quote?.getAlertBelow() ?: 0f
    if (alertBelow != 0.0f) {
      alertBelowInputEditText.setText(Quote.selectedFormat.format(alertBelow))
    } else {
      alertBelowInputEditText.setText("")
    }

    addButton.setOnClickListener { onAddClicked() }
  }

  private fun onAddClicked() {
    val alertAboveText = alertAboveInputEditText.text.toString()
    val alertBelowText = alertBelowInputEditText.text.toString()
    var alertAbove = 0f
    var alertBelow = 0f
    var success = true

    if (alertAboveText.isNotEmpty()) {
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        alertAbove = numberFormat.parse(alertAboveText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        alertAboveInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
    }

    if (alertBelowText.isNotEmpty()) {
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        alertBelow = numberFormat.parse(alertBelowText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        alertBelowInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
    }

    if (alertAbove > 0.0f && alertBelow > 0.0f) {
      if (alertAboveInputEditText.isFocused) {
        if (success && alertBelow >= alertAbove) {
          alertAboveInputLayout.error = getString(R.string.alert_below_error)
          success = false
        }
      } else {
        if (success && alertBelow >= alertAbove) {
          alertBelowInputLayout.error = getString(R.string.alert_above_error)
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
    val quote = checkNotNull(viewModel.quote)
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }
}