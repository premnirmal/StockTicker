package com.github.premnirmal.ticker.portfolio

import android.os.Bundle
import com.github.premnirmal.ticker.BaseActivity
import com.github.premnirmal.ticker.Injector
import com.github.premnirmal.ticker.model.IStocksProvider
import com.github.premnirmal.tickerwidget.R
import kotlinx.android.synthetic.main.activity_positions.doneButton
import kotlinx.android.synthetic.main.activity_positions.price
import kotlinx.android.synthetic.main.activity_positions.shares
import kotlinx.android.synthetic.main.activity_positions.skipButton
import kotlinx.android.synthetic.main.activity_positions.tickerName
import kotlinx.android.synthetic.main.activity_positions.toolbar
import javax.inject.Inject

/**
 * Created by premnirmal on 2/25/16.
 */
open class AddPositionActivity : BaseActivity() {

  companion object {
    const val TICKER = "TICKER"
  }

  @Inject
  lateinit internal var stocksProvider: IStocksProvider
  lateinit protected var ticker: String

  public override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    Injector.inject(this)
    ticker = intent.getStringExtra(TICKER)
    setContentView(R.layout.activity_positions)
    updateToolbar(toolbar)
    toolbar.setNavigationOnClickListener {
      finish()
    }
    val name = tickerName
    name.text = ticker

    doneButton.setOnClickListener { onDoneClicked() }

    skipButton.setOnClickListener { skip() }

    price.addTextChangedListener(PriceTextChangeListener(price))
  }

  protected open fun skip() {
    finish()
  }

  protected fun onDoneClicked() {
    val sharesView = shares
    val priceView = price
    val priceText = priceView.text.toString().substring(1)
    val sharesText = sharesView.text.toString()
    if (!priceText.isEmpty() && !sharesText.isEmpty()) {
      val price = java.lang.Float.parseFloat(priceText)
      val shares = java.lang.Integer.parseInt(sharesText)
      stocksProvider.addPosition(ticker, shares, price)
    }
    finish()
  }
}