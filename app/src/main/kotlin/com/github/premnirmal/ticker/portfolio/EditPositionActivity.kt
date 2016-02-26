package com.github.premnirmal.ticker.portfolio

import android.content.DialogInterface
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import com.github.premnirmal.tickerwidget.R

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
            val sharesView = findViewById(R.id.shares) as EditText
            val priceView = findViewById(R.id.price) as EditText
            val skipButton = findViewById(R.id.skipButton) as Button
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