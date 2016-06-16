package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Bundle
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.*

/**
 * Created by premnirmal on 2/25/16.
 */
class EditPositionActivity : AddPositionActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    val stock = stocksProvider.getStock(ticker)
    if (stock != null) {
      val sharesView = shares
      val priceView = price
      val skipButton = skipButton
      skipButton.setText(R.string.remove)
      sharesView.setText("" + stock.PositionShares)
      priceView.setText("" + stock.PositionPrice)
    } else {
      showDialog(getString(R.string.no_such_stock_in_portfolio),
          DialogInterface.OnClickListener { p0, p1 -> finish() })
    }
  }

  override fun skip() {
    stocksProvider.removePosition(ticker)
    super.skip()
  }
}