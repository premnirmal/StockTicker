package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Quote
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_alerts.alertAboveInputEditText
import kotlinx.android.synthetic.main.activity_alerts.alertAboveInputLayout
import kotlinx.android.synthetic.main.activity_alerts.alertBelowInputEditText
import kotlinx.android.synthetic.main.activity_alerts.alertBelowInputLayout
import kotlinx.android.synthetic.main.activity_notes.addButton
import kotlinx.android.synthetic.main.activity_notes.tickerName
import kotlinx.android.synthetic.main.activity_notes.toolbar
import java.text.NumberFormat
import javax.inject.Inject

class AddAlertsActivity : BaseActivity() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  internal lateinit var ticker: String
  override val simpleName: String = "AddAlertsActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_alerts)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    tickerName.text = ticker

    val quote = stocksProvider.getStock(ticker)
    val alertAbove = quote?.getAlertAbove()
    alertAboveInputEditText.setText(Quote.selectedFormat.format(alertAbove))
    val alertBelow = quote?.getAlertBelow()
    alertBelowInputEditText.setText(Quote.selectedFormat.format(alertBelow))

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

    if (alertAbove!! > 0.0f && alertBelow!! > 0.0f) {
      if (alertAboveInputEditText.isFocused) {
        if (success && alertBelow != null && alertBelow!! >= alertAbove) {
          alertAboveInputLayout.error = getString(R.string.alert_below_error)
          success = false
        }
      } else {
        if (success && alertAbove != null && alertBelow >= alertAbove!!) {
          alertBelowInputLayout.error = getString(R.string.alert_above_error)
          success = false
        }
      }
    }
    if (success) {
      stocksProvider.upsertAlertAbove(ticker, alertAbove)
      stocksProvider.upsertAlertBelow(ticker, alertBelow)
      updateActivityResult()
      dismissKeyboard()
      finish()
    }
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }
}