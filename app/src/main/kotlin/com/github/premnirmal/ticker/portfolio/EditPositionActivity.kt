package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Bundle
import com.github.premnirmal.ticker.Tools
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.shares
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
      if (stock.positionShares > 0 && stock.positionPrice > 0) {
        val sharesView = shares
        val priceView = price
        val skipButton = skipButton
        skipButton.setText(R.string.remove)
        sharesView.setText(stock.positionShares.toString())
        val value = Tools.DECIMAL_FORMAT.format(stock.positionPrice)
        priceView.setText(value)
        priceView.setSelection(value.length)
      }
    } else {
      showDialog(getString(R.string.no_such_stock_in_portfolio),
          DialogInterface.OnClickListener { _, _ -> finish() })
    }
  }

  override fun skip() {
    stocksProvider.removePosition(ticker)
    super.skip()
  }
}