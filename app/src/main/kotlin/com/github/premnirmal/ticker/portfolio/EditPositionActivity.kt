package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Bundle
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
      val sharesView = shares
      val priceView = price
      val skipButton = skipButton
      skipButton.setText(R.string.remove)
      sharesView.setText(stock.PositionShares.toString())
      val value = stock.PositionPrice.toString()
      val priceString: String
      if (value.length > 3 && value.toCharArray()[value.length - 2] == '.') {
        priceString = "${value}0"
      } else {
        priceString = value
      }
      priceView.setText(priceString)
    } else {
      showDialog(getString(R.string.no_such_stock_in_portfolio),
          DialogInterface.OnClickListener { dialog, which -> finish() })
    }
  }

  override fun skip() {
    stocksProvider.removePosition(ticker)
    super.skip()
  }
}