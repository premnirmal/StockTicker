package com.github.premnirmal.ticker.portfolio

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.ticker.base.BaseActivity
import com.github.premnirmal.ticker.components.InAppMessage
import com.github.premnirmal.ticker.components.Injector
import com.github.premnirmal.ticker.dismissKeyboard
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.ticker.network.data.Holding
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.addButton
import kotlinx.android.synthetic.main.activity_positions.averagePrice
import kotlinx.android.synthetic.main.activity_positions.positionsHolder
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.priceInputLayout
import kotlinx.android.synthetic.main.activity_positions.shares
import kotlinx.android.synthetic.main.activity_positions.sharesInputLayout
import kotlinx.android.synthetic.main.activity_positions.tickerName
import kotlinx.android.synthetic.main.activity_positions.toolbar
import kotlinx.android.synthetic.main.activity_positions.totalShares
import kotlinx.android.synthetic.main.activity_positions.totalValue
import kotlinx.android.synthetic.main.layout_position_holding.view.remove_position
import java.text.NumberFormat
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
class AddPositionActivity : BaseActivity() {

  companion object {
    const val QUOTE = "QUOTE"
    const val TICKER = "TICKER"
  }

  @Inject internal lateinit var stocksProvider: IStocksProvider
  internal lateinit var ticker: String
  override val simpleName: String = "AddPositionActivity"

  override fun onCreate(savedInstanceState: Bundle?) {
    Injector.appComponent.inject(this)
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_positions)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    if (intent.hasExtra(TICKER) && intent.getStringExtra(TICKER) != null) {
      ticker = intent.getStringExtra(TICKER)!!
    } else {
      ticker = ""
      InAppMessage.showToast(this, R.string.error_symbol)
      finish()
      return
    }
    tickerName.text = ticker

    addButton.setOnClickListener { onAddClicked() }

    positionsHolder.removeAllViews()
    val position = stocksProvider.getPosition(ticker)
    position?.let {
      for (holding in position.holdings) {
        addPositionView(holding)
      }
    }
    updateTotal()
  }

  private fun onAddClicked() {
    val sharesView = shares
    val priceView = price
    val priceText = priceView.text.toString()
    val sharesText = sharesView.text.toString()
    if (priceText.isNotEmpty() && sharesText.isNotEmpty()) {
      var price = 0f
      var shares = 0f
      var success = true
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        price = numberFormat.parse(priceText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        priceInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      try {
        val numberFormat: NumberFormat = NumberFormat.getNumberInstance()
        shares = numberFormat.parse(sharesText)!!
            .toFloat()
      } catch (e: NumberFormatException) {
        sharesInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      // Check for zero shares.
      if (success) {
        if (shares == 0.0f) {
          sharesInputLayout.error = getString(R.string.invalid_number)
          success = false
        }
      }
      if (success) {
        priceInputLayout.error = null
        sharesInputLayout.error = null
        val holding = stocksProvider.addHolding(ticker, shares, price)
        priceView.setText("")
        sharesView.setText("")
        addPositionView(holding)
        updateTotal()
        updateActivityResult()
      }
    }
    dismissKeyboard()
  }

  private fun updateActivityResult() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    val data = Intent()
    data.putExtra(QUOTE, quote)
    setResult(Activity.RESULT_OK, data)
  }

  private fun addPositionView(holding: Holding) {
    val view = layoutInflater.inflate(R.layout.layout_position_holding, null)
    val positionNumShares = view.findViewById<TextView>(R.id.positionShares)
    val positionPrice = view.findViewById<TextView>(R.id.positionPrice)
    val positionTotalValue = view.findViewById<TextView>(R.id.positionTotalValue)
    positionNumShares.text = AppPreferences.DECIMAL_FORMAT.format(holding.shares)
    positionPrice.text = AppPreferences.DECIMAL_FORMAT.format(holding.price)
    positionTotalValue.text = AppPreferences.DECIMAL_FORMAT.format(holding.totalValue())
    positionsHolder.addView(view)
    view.tag = holding
    // Remove entry when right side 'x' icon is clicked.
    view.remove_position.setOnClickListener {
      AlertDialog.Builder(this)
          .setTitle(R.string.remove)
          .setMessage(getString(R.string.remove_holding, "${holding.shares}@${holding.price}"))
          .setPositiveButton(R.string.remove) { dialog, _ ->
            stocksProvider.removePosition(ticker, holding)
            positionsHolder.removeView(view)
            updateTotal()
            dialog.dismiss()
          }
          .setNegativeButton(R.string.cancel) { dialog, _ -> dialog.dismiss() }
          .show()
    }
  }

  private fun updateTotal() {
    val quote = checkNotNull(stocksProvider.getStock(ticker))
    totalShares.text = quote.numSharesString()
    averagePrice.text = quote.averagePositionPrice()
    totalValue.text = quote.totalSpentString()
  }
}