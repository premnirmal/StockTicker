package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Bundle
import android.view.View
import com.github.premnirmal.ticker.AppPreferences
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.addRemoveText
import kotlinx.android.synthetic.main.activity_positions.averagePositionPrice
import kotlinx.android.synthetic.main.activity_positions.currentPosition
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.priceInputLayout
import kotlinx.android.synthetic.main.activity_positions.removeButton
import kotlinx.android.synthetic.main.activity_positions.shares
import kotlinx.android.synthetic.main.activity_positions.sharesInputLayout
import kotlinx.android.synthetic.main.activity_positions.skipButton
import kotlinx.android.synthetic.main.activity_positions.toolbar

/**
 * Created by premnirmal on 2/25/16.
 */
class EditPositionActivity : AddPositionActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    toolbar.setTitle(R.string.edit_position)
    val stock = stocksProvider.getStock(ticker)
    if (stock != null) {
      if (stock.isPosition) {
        currentPosition.text = stock.totalPosition.toString()
        averagePositionPrice.text = String.format("%.2f",stock.averagePositionPrice);
        removeButton.setOnClickListener{ onRemoveClicked() }
        removeButton.visibility = View.VISIBLE
        addRemoveText.setText(R.string.add_remove_position)
      }
    } else {
      showDialog(getString(R.string.no_such_stock_in_portfolio),
          DialogInterface.OnClickListener { _, _ -> finish() })
    }
  }

  protected fun onRemoveClicked() {
    val sharesView = shares
    val sharesText = sharesView.text.toString()
    if (!sharesText.isEmpty()) {
      var shares = 0f
      var success = true
      try {
        shares = sharesText.toFloat()
      } catch (e: NumberFormatException) {
        sharesInputLayout.error = getString(R.string.invalid_number)
        success = false
      }
      if (success) {
        priceInputLayout.error = null
        sharesInputLayout.error = null
        stocksProvider.decreasePosition(ticker, shares)
        finish()
      }
    }
  }
}